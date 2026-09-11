package org.mycompany.strategy;

import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;
import org.mycompany.persistence.ImportRepository;

import java.util.stream.Stream;

/**
 * Full replace mode: clears target table/folder, then writes all records.
 * Used when source represents the complete desired state.
 * Writes are committed per record after initial table/folder clear.
 */
public class ReplaceFolderStrategy implements UpdateStrategy {

    @Override
    public String name() {
        return "replaceFolder";
    }

    @Override
    public Stream<WriteResult> apply(Stream<MappedRecord> records, ImportRepository repository) {
        repository.clearTargetFolder();
        return records.map(repository::write);
    }
}
