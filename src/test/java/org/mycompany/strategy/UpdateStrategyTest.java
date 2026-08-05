package org.mycompany.strategy;

import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;
import org.mycompany.persistence.CmsRepository;

import org.junit.jupiter.api.Test;
import org.mycompany.strategy.ReplaceFolderStrategy;
import org.mycompany.strategy.UpdateByIdStrategy;
import org.mycompany.strategy.UpdateStrategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Strategy tests using an in-memory CmsRepository stub.
 * No JCR, no Mockito — focuses purely on strategy behavior.
 */
class UpdateStrategyTest {

    @Test
    void replaceFolderClearsThenWritesEachRecord() {
        StubRepository repo = new StubRepository();
        UpdateStrategy strategy = new ReplaceFolderStrategy();

        List<WriteResult> results = strategy
                .apply(Stream.of(record(1, "Shop A"), record(2, "Shop B")), repo)
                .toList();

		// Must clear before any writes
        assertEquals(List.of("clear", "write", "write"), repo.actionLog);

		// Both records written successfully
        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());
    }

    @Test
    void updateByIdWritesNewAndRejectsExisting() {
        StubRepository repo = new StubRepository();
        repo.markAsExisting("Shop A");   // A exists, B not
        UpdateStrategy strategy = new UpdateByIdStrategy();

        List<WriteResult> results = strategy
                .apply(Stream.of(record(1, "Shop A"), record(2, "Shop B")), repo)
                .toList();

		// Must not clear target folder (non-destructive strategy)
        assertFalse(repo.actionLog.contains("clear"));

		// Existing record rejected (A)
        assertFalse(results.get(0).isSuccess());
        assertTrue(results.get(0).error().contains("existiert bereits"));

		// New record written once (B)
        assertTrue(results.get(1).isSuccess());
        assertEquals(1, repo.actionLog.stream().filter("write"::equals).count());
    }

    @Test
    void strategyNamesMatchYamlVocabulary() {
		// Names must match YAML keys for registry lookup
        assertEquals("replaceFolder", new ReplaceFolderStrategy().name());
        assertEquals("updateById",    new UpdateByIdStrategy().name());
    }

    // --- Helpers ---------------------------------------------------------

    private static MappedRecord record(int rowNumber, String shopName) {
        return new MappedRecord(rowNumber, "ht:wheretobuydocument",
                Map.of("ht:name", shopName));
    }

	/** Simple in-memory CmsRepository stub for isolated tests. */
    private static class StubRepository implements CmsRepository {
        final List<String> actionLog = new ArrayList<>();
        private final Set<String> existingNames = new HashSet<>();
        private final AtomicInteger pathCounter = new AtomicInteger(0);

        void markAsExisting(String name) {
            existingNames.add(name);
        }

        @Override
        public WriteResult write(MappedRecord record) {
            actionLog.add("write");
            String path = "/content/test/doc-" + pathCounter.incrementAndGet();
            return WriteResult.success(record.rowNumber(), path);
        }

        @Override
        public void clearTargetFolder() {
            actionLog.add("clear");
        }

        @Override
        public boolean exists(MappedRecord record) {
            String name = (String) record.properties().get("ht:name");
            return existingNames.contains(name);
        }
    }
}
