package org.mycompany.enrichment;

import org.mycompany.model.MappedRecord;

import java.util.Optional;

/**
 * Applies enrichment rules to a record.
 * Returns empty if a SKIP_DATASET rule fails.
 * Throws RuntimeException if a FAIL_JOB rule fails.
 */
public interface EnrichmentService {

    Optional<MappedRecord> enrich(MappedRecord record);
}
