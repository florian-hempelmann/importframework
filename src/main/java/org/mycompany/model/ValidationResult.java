package org.mycompany.model;

import java.util.List;

/**
 * Validation result for a single record.
 * Valid: failures empty, isValid() true.
 * Invalid: failures lists all violations (no fail-fast).
 */
public record ValidationResult(int rowNumber, List<ValidationFailure> failures) {

    public ValidationResult {
        failures = List.copyOf(failures);
    }

    public static ValidationResult valid(int rowNumber) {
        return new ValidationResult(rowNumber, List.of());
    }

    public static ValidationResult invalid(int rowNumber, List<ValidationFailure> failures) {
        if (failures.isEmpty()) {
            throw new IllegalArgumentException("invalid() needs at least one failure");
        }
        return new ValidationResult(rowNumber, failures);
    }

    public boolean isValid() {
        return failures.isEmpty();
    }
}
