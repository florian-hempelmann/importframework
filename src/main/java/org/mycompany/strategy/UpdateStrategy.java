package org.mycompany.strategy;

import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;
import org.mycompany.persistence.ImportRepository;

import java.util.stream.Stream;

/**
 * Strategy used to update the CMS target.
 * Determined either by user input or the default defined in the mapping YAML.
 */
public interface UpdateStrategy {

    String name();

    /**
     * Strategy operating on a stream of MappedRecords and producing WriteResults.
	 * Stream parsing and consumption are orchestrated by ImportService.java.
     */
    Stream<WriteResult> apply(Stream<MappedRecord> records, ImportRepository repository);
}
