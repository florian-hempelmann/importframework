package org.mycompany.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Record mapped to JCR properties and ready for persistence.
 * Properties contain converted values matching the target node type.
 */
public record MappedRecord(int rowNumber, String nodeType, Map<String, Object> properties) {

    public MappedRecord {
        properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
    }
}
