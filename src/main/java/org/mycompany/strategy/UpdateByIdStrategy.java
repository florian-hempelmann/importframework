package org.mycompany.strategy;

import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;
import org.mycompany.persistence.ImportRepository;

import java.util.stream.Stream;

/**
 * Upsert mode: checks if record exists by matchBy.
 * If not found → create, if found → marked as error.
 * TODO: Full update not implemented in PoC due to workflow constraints.
 */
public class UpdateByIdStrategy implements UpdateStrategy {

    @Override
    public String name() {
        return "updateById";
    }

    @Override
    public Stream<WriteResult> apply(Stream<MappedRecord> records, ImportRepository repository) {
        return records.map(record -> {
            if (repository.exists(record)) {
                return WriteResult.failure(record.rowNumber(),
                        "Document already exists; in-place update ist not implemented for PoC");
            }
            return repository.write(record);
        });
    }
}
