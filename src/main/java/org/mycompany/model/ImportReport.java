package org.mycompany.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable report of an import run.
 * Created by the ImportService and returned to the REST layer.
 */
public record ImportReport(
    String type,
    String strategy,
    Instant startedAt,
    Instant finishedAt,
    String executedBy,
    Totals totals,
    List<ReportFailure> failures
) {

    public ImportReport {
        failures = List.copyOf(failures);
    }

    public boolean hasFailures() {
        return totals.failed() > 0;
    }

	/**
	 * Import counters. read is tracked explicitly to detect incomplete runs.
	 */
    public record Totals(int read, int succeeded, int failed, int skipped) { }

	/**
	 * Failure details for a single source row, including validation or persistence errors.
	 */
    public record ReportFailure(int rowNumber, String reason) { }

    public static Builder builder(String type, String strategy, String executedBy) {
        return new Builder(type, strategy, executedBy);
    }

	/**
	 * Mutable accumulator used during an import run.
	 * Produces an immutable report via build().
	 */
    public static final class Builder {

        private final String type;
        private final String strategy;
        private final String executedBy;
        private final Instant startedAt;

        private int read;
        private int succeeded;
        private int failed;
        private int skipped;

        private final List<ReportFailure> failures;

        private Builder(String type, String strategy, String executedBy) {
            this.type = type;
            this.strategy = strategy;
            this.executedBy = executedBy;
            this.startedAt = Instant.now();
            this.failures = new ArrayList<>();
        }

        public void addRead() {
            read++;
        }

        public void addSuccess() {
            succeeded++;
        }

        public void addFailure(int rowNumber, String reason) {
            failed++;
            failures.add(new ReportFailure(rowNumber, reason));
        }

        public void addSkip() {
            skipped++;
        }

        public ImportReport build() {
            return new ImportReport(
                type,
                strategy,
                startedAt,
                Instant.now(),
                executedBy,
                new Totals(read, succeeded, failed, skipped),
                failures
            );
        }
    }
}
