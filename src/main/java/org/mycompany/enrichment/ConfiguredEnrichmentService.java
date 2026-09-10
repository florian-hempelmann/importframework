package org.mycompany.enrichment;

import org.mycompany.config.MappingConfig;
import org.mycompany.config.MappingConfig.ColumnMapping;
import org.mycompany.config.MappingConfig.EnrichmentRule;
import org.mycompany.config.MappingConfig.OnFailure;
import org.mycompany.model.MappedRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Executes the enrichment rules from a MappingConfig.
 * Currently dispatches by rule.service():
 *   - "geocoding"   → GeocodingService (skipped if not configured)
 * Unknown services raise IllegalStateException up the call stack.
 *
 * Each rule's OnFailure setting governs error handling:
 *   - SKIP_DATASET               → return Optional.empty
 *   - CONTINUE_WITHOUT_ENRICHMENT → log warning, continue with next rule
 *   - FAIL_JOB                    → rethrow
 */
public class ConfiguredEnrichmentService implements EnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguredEnrichmentService.class);

    private static final String SERVICE_GEOCODING = "geocoding";
    private static final String FIELD_ADDRESS  = "address";
    private static final String FIELD_ZIPCODE  = "zipcode";
    private static final String FIELD_CITY     = "city";
    private static final String FIELD_COUNTRY  = "country";
    private static final String PROP_LATITUDE  = "latitude";
    private static final String PROP_LONGITUDE = "longitude";

    private final MappingConfig config;
    private final GeocodingService geocoder;
    private final Map<String, String> sourceToTarget;

    public ConfiguredEnrichmentService(MappingConfig config, GeocodingService geocoder) {
        this.config = config;
        this.geocoder = geocoder;
        this.sourceToTarget = config.columns().stream()
                .collect(Collectors.toUnmodifiableMap(
                        ColumnMapping::sourceName, ColumnMapping::targetProperty));
    }

    @Override
    public Optional<MappedRecord> enrich(MappedRecord record) {
        if (config.enrichment().isEmpty()) {
            return Optional.of(record);
        }

        Map<String, Object> properties = new LinkedHashMap<>(record.properties());

        for (EnrichmentRule rule : config.enrichment()) {
            // Pre-normalize target values to avoid type conflicts in JCR (CND schema).
            normalizeTargetProperties(rule, properties);

            if (!ruleApplies(rule, properties)) {
                continue;
            }
            try {
                applyRule(rule, properties);
            } catch (RuntimeException e) {
                OnFailure onFailure = rule.onFailure();
                switch (onFailure) {
                    case SKIP_DATASET -> {
                        log.warn("Row {}: enrichment '{}' failed, skipping dataset: {}",
                                record.rowNumber(), rule.service(), e.getMessage());
                        return Optional.empty();
                    }
                    case CONTINUE_WITHOUT_ENRICHMENT ->
                        log.warn("Row {}: enrichment '{}' failed, continuing without it: {}",
                                record.rowNumber(), rule.service(), e.getMessage());
                    case FAIL_JOB -> {
                        log.error("Row {}: enrichment '{}' failed with FAIL_JOB policy",
                                record.rowNumber(), rule.service());
                        throw e;
                    }
                }
            }
        }
        return Optional.of(new MappedRecord(record.rowNumber(), properties));
    }

	/**
	 * Checks if the rule's optional requiresProperty is enabled.
	 * True if not configured or set to a truthy value.
	 */
    private static boolean ruleApplies(EnrichmentRule rule, Map<String, Object> properties) {
        String requires = rule.requiresProperty();
        if (requires == null || requires.isBlank()) {
            return true;
        }
        return isTruthy(properties.get(requires));
    }

    private static boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String s = value.toString().trim();
        return "true".equalsIgnoreCase(s) || "x".equalsIgnoreCase(s);
    }

    private void applyRule(EnrichmentRule rule, Map<String, Object> properties) {
        switch (rule.service()) {
            case SERVICE_GEOCODING -> applyGeocoding(rule, properties);
            default -> throw new IllegalStateException(
                    "Unknown enrichment service: " + rule.service());
        }
    }

    /**
     * Normalizes target properties by type.
     * For geocoding, converts String values to Double or drops invalid ones.
     */
    private static void normalizeTargetProperties(EnrichmentRule rule, Map<String, Object> properties) {
        if (!SERVICE_GEOCODING.equals(rule.service())) {
            return;
        }
        for (String target : rule.targetProperties().values()) {
            if (target == null) {
                continue;
            }
            Object value = properties.get(target);
            if (value == null || value instanceof Number || value instanceof Boolean) {
                continue;
            }
            Double parsed = parseCoordinate(value);
            if (parsed != null) {
                properties.put(target, parsed);
            } else {
                properties.remove(target);
            }
        }
    }

    private void applyGeocoding(EnrichmentRule rule, Map<String, Object> properties) {
        String latProp = rule.targetProperties().get(PROP_LATITUDE);
        String lngProp = rule.targetProperties().get(PROP_LONGITUDE);

        // After normalize, both slots are either typed Double or absent — never String.
        if (properties.get(latProp) instanceof Double && properties.get(lngProp) instanceof Double) {
            return;
        }

        if (geocoder == null) {
            throw new GeocodingException("No geocoding service configured (missing API key?)");
        }

        GeocodeRequest request = new GeocodeRequest(
                readSourceProperty(properties, rule, FIELD_ADDRESS),
                readSourceProperty(properties, rule, FIELD_ZIPCODE),
                readSourceProperty(properties, rule, FIELD_CITY),
                readSourceProperty(properties, rule, FIELD_COUNTRY));

        Coordinates coords = geocoder.geocode(request);

        writeTargetProperty(properties, rule, PROP_LATITUDE,  coords.lat());
        writeTargetProperty(properties, rule, PROP_LONGITUDE, coords.lng());
    }

	/**
	 * Parses a valid coordinate value.
	 * Ignores default/sentinel values like 0.0 and -1.0.
	 * Supports comma decimal separators.
	 */
    private static Double parseCoordinate(Object value) {
        if (value == null) {
            return null;
        }
        String s = value.toString().trim().replace(',', '.');
        if (s.isEmpty()) {
            return null;
        }
        try {
            double d = Double.parseDouble(s);
            if (d == 0.0 || d == -1.0) {
                return null;
            }
            return d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Reads a source-field value from the mapped properties.
     * The rule references source-column names; we resolve them to JCR
     * target keys via the column mapping.
     */
    private String readSourceProperty(Map<String, Object> properties,
                                      EnrichmentRule rule, String sourceField) {
        if (!rule.sourceFields().contains(sourceField)) {
            return null;
        }
        String targetKey = sourceToTarget.get(sourceField);
        if (targetKey == null) {
            return null;
        }
        Object value = properties.get(targetKey);
        return value == null ? null : value.toString();
    }

    private static void writeTargetProperty(Map<String, Object> properties,
                                            EnrichmentRule rule, String logicalName, Object value) {
        String targetKey = rule.targetProperties().get(logicalName);
        if (targetKey == null) {
            // Logical name not mapped — silently ignore (rule may omit some outputs).
            return;
        }
        properties.put(targetKey, value);
    }
}
