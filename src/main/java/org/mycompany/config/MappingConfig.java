package org.mycompany.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Typed in-memory representation of a YAML mapping config.
 * Loaded per import type and used by mapping, enrichment, and persistence.
 */
public record MappingConfig(
    String type,
    Target target,
    Strategy strategy,
    List<ColumnMapping> columns,
    List<EnrichmentRule> enrichment
) {

    public MappingConfig {
        columns    = List.copyOf(columns);
        enrichment = enrichment == null ? List.of() : List.copyOf(enrichment);
    }

	/**
	 * JCR target path and document type. No country code.
	 */
    public record Target(String table) { }

	/**
	 * Update strategy per import type.
	 * defaultStrategy maps YAML "default".
	 * matchBy defines the column used for update lookup.
	 */
    public record Strategy(
        @JsonProperty("default") String defaultStrategy,
        String matchBy
    ) { }

    public record ColumnMapping(
        String sourceName,
        String targetProperty,
		String firstOccurrenceFlag,
		List<String> validators
    ) {  public ColumnMapping {
		validators = validators == null ? List.of() : List.copyOf(validators);
	} }

	/**
	 * Enrichment rule for an external service.
	 * sourceFields: input columns
	 * targetProperties: service result → JCR mapping
	 * onFailure: behavior if enrichment fails
	 * requiresProperty: optional JCR property name — rule only applies
	 *                   when this property is truthy (Boolean.TRUE, "true", "x").
	 */
    public record EnrichmentRule(
        String service,
        List<String> sourceFields,
        Map<String, String> targetProperties,
        OnFailure onFailure,
        String requiresProperty
    ) {
        public EnrichmentRule {
            sourceFields     = List.copyOf(sourceFields);
            targetProperties = Map.copyOf(targetProperties);
        }
    }

	/**
	 * Enrichment failure behavior (mapped from YAML values).
	 */
    public enum OnFailure {
        @JsonProperty("skipDataset")               SKIP_DATASET,
        @JsonProperty("continueWithoutEnrichment") CONTINUE_WITHOUT_ENRICHMENT,
        @JsonProperty("failJob")                   FAIL_JOB
    }
}
