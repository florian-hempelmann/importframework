package org.mycompany.service;

import org.mycompany.config.ConfigLoader;
import org.mycompany.config.MappingConfig;
import org.mycompany.enrichment.ConfiguredEnrichmentService;
import org.mycompany.enrichment.EnrichmentService;
import org.mycompany.enrichment.GeocodingService;
import org.mycompany.mapping.MappingService;
import org.mycompany.mapping.Validator;
import org.mycompany.model.ImportReport;
import org.mycompany.model.MappedRecord;
import org.mycompany.model.Record;
import org.mycompany.model.ValidationResult;
import org.mycompany.parser.Parser;
import org.mycompany.parser.ParserFactory;
import org.mycompany.persistence.CmsRepository;
import org.mycompany.persistence.JackrabbitRepositoryAdapter;
import org.mycompany.strategy.UpdateStrategy;

import javax.jcr.Session;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orchestrates the import pipeline: Parser → Mapping → Strategy → Persistence.
 * Produces an ImportReport per run.
 *
 * Long-lived dependencies: ConfigLoader, ParserFactory, validators, strategies.
 * Per-run dependencies: MappingService, CmsRepository, JCR Session.
 */
public class ImportService {

    private final ConfigLoader configLoader;
    private final ParserFactory parserFactory;
    private final List<Validator> validators;
    private final Map<String, UpdateStrategy> strategyRegistry;
    private final GeocodingService geocoder;

    public ImportService(
            ConfigLoader configLoader,
            ParserFactory parserFactory,
            List<Validator> validators,
            List<UpdateStrategy> strategies,
            GeocodingService geocoder) {
        this.configLoader = configLoader;
        this.parserFactory = parserFactory;
        this.validators = List.copyOf(validators);
        this.strategyRegistry = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(UpdateStrategy::name, s -> s));
        // geocoder may be null when no API key is configured; enrichment then
        // either skips (CONTINUE_WITHOUT_ENRICHMENT) or fails per its OnFailure setting.
        this.geocoder = geocoder;
    }

	/**
	 * Executes a full import run.
	 *
	 * @param type        Import type (drives YAML lookup, e.g. "wheretobuy")
	 * @param filename    Source filename (determines parser by extension)
	 * @param content     Input stream of the uploaded file
	 * @param session     JCR session of the requesting user
	 * @param executedBy  User identity used for audit logging in the report
	 */
    public ImportReport runImport(
            String type,
            String filename,
            InputStream content,
            Session session,
            String executedBy) {

        MappingConfig config = configLoader.load(type);
        UpdateStrategy strategy = resolveStrategy(config);
        Parser parser = parserFactory.forFile(filename);

        MappingService mappingService = new MappingService(config, validators);
        EnrichmentService enrichmentService = new ConfiguredEnrichmentService(config, geocoder);
        CmsRepository repository = new JackrabbitRepositoryAdapter(session, config);

        ImportReport.Builder report = ImportReport.builder(type, strategy.name(), executedBy);

        List<MappedRecord> validRecords =
                parseAndValidate(parser, content, mappingService, report);

        if (validRecords.isEmpty()) {
			// Records that passed mapping and validation successfully.
			// Only these are passed to enrichment and persistence.
            return report.build();
        }

        List<MappedRecord> enrichedRecords =
                enrich(validRecords, enrichmentService, report);

        if (enrichedRecords.isEmpty()) {
            return report.build();
        }

        applyStrategyAndCollectResults(strategy, enrichedRecords, repository, report);

        return report.build();
    }

    // --- Pipeline-Steps -----------------------------------------------

    private UpdateStrategy resolveStrategy(MappingConfig config) {
        String name = config.strategy().defaultStrategy();
        UpdateStrategy strategy = strategyRegistry.get(name);
        if (strategy == null) {
            throw new IllegalStateException(
                    "Unbekannte Strategy '" + name + "'. Registriert sind: "
                            + strategyRegistry.keySet());
        }
        return strategy;
    }

    private static List<MappedRecord> parseAndValidate(
            Parser parser,
            InputStream content,
            MappingService mappingService,
            ImportReport.Builder report) {

        List<MappedRecord> valid = new ArrayList<>();

        try (Stream<Record> records = parser.parse(content)) {
            records.forEach(record -> {
                report.addRead();
                MappingService.Result result = mappingService.map(record);
                if (result instanceof MappingService.Mapped m) {
                    valid.add(m.record());
                } else if (result instanceof MappingService.Rejected r) {
                    ValidationResult vr = r.validationResult();
                    report.addFailure(vr.rowNumber(), formatFailures(vr));
                }
            });
        }
        return valid;
    }

    private static List<MappedRecord> enrich(
            List<MappedRecord> validRecords,
            EnrichmentService enrichmentService,
            ImportReport.Builder report) {

        List<MappedRecord> enriched = new ArrayList<>(validRecords.size());
        for (MappedRecord record : validRecords) {
            enrichmentService.enrich(record).ifPresentOrElse(
                    enriched::add,
                    () -> report.addFailure(record.rowNumber(), "enrichment skipped dataset"));
        }
        return enriched;
    }

    private static void applyStrategyAndCollectResults(
            UpdateStrategy strategy,
            List<MappedRecord> validRecords,
            CmsRepository repository,
            ImportReport.Builder report) {

        strategy.apply(validRecords.stream(), repository).forEach(writeResult -> {
            if (writeResult.isSuccess()) {
                report.addSuccess();
            } else {
                report.addFailure(writeResult.rowNumber(), writeResult.error());
            }
        });
    }

	/**
	 * Formats validation errors into a single string per row.
	 */
    private static String formatFailures(ValidationResult vr) {
        return vr.failures().stream()
                .map(f -> f.columnName() + ": " + f.reason())
                .collect(Collectors.joining("; "));
    }
}
