package org.mycompany.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Parsed source row independent of file format or import type.
 * Field lookup is case-insensitive.
 */

public record Record(int rowNumber, Map<String, String> fields) {

    public Record {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public Optional<String> get(String columnName) {
        if (columnName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fields.get(columnName.toLowerCase(Locale.ROOT)));
    }
}
