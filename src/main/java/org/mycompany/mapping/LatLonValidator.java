package org.mycompany.mapping;

import org.mycompany.model.ValidationFailure;

import java.util.Optional;

/**
 * Validator for `validate: lat-lon`.
 * Checks that the value is a decimal in range -180..180.
 * Empty values are allowed (handled by RequiredValidator).
 */
public class LatLonValidator implements Validator {

    private static final double MIN = -180.0;
    private static final double MAX = 180.0;

    @Override
    public String name() {
        return "lat-lon";
    }

    @Override
    public Optional<ValidationFailure> validate(String columnName, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            double d = Double.parseDouble(value);
            if (d < MIN || d > MAX) {
                return Optional.of(new ValidationFailure(columnName, name(),
                        "Value: " + value + " not in: " + MIN + ".." + MAX));
            }
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.of(new ValidationFailure(columnName, name(),
                    "'" + value + "' is not a valid decimal number"));
        }
    }
}
