package org.mycompany.persistence;

import org.mycompany.config.MappingConfig;
import org.mycompany.config.MappingConfig.ColumnMapping;
import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter between the importer model
 * and a JCR repository implementation.
 *
 * Flow per record:
 * 1. Resolve target folder from config
 * 2. Create document via FolderWorkflow (handle + variant)
 * 3. Apply properties to variant node
 * 4. Set CMS display name via DefaultWorkflow
 * 5. session.save() persists changes + triggers audit metadata
 *
 * One save() per record = transaction boundary.
 * Failures are isolated per record.
 */
public class JackrabbitRepositoryAdapter implements CmsRepository {

    private final Session session;
    private final MappingConfig config;
    private final String matchByTargetProperty;

    public JackrabbitRepositoryAdapter(Session session, MappingConfig config) {
        this.session = session;
        this.config = config;
        this.matchByTargetProperty = resolveMatchByTargetProperty(config);
    }

    @Override
    public WriteResult write(MappedRecord record) {
        try {
            String folderPath = config.target().jcrPath();
            if (!session.nodeExists(folderPath)) {
                return WriteResult.failure(record.rowNumber(),
                        "Zielordner existiert nicht: " + folderPath);
            }

            Node folderNode = session.getNode(folderPath);
            String nodeName = computeNodeName(record);
            String displayName = computeDisplayName(record, nodeName);

            Node document = folderNode.addNode(nodeName);
            document.setProperty("displayName", displayName);

            setProperties(document, record.properties());

            session.save();

            return WriteResult.success(record.rowNumber(), document.getPath());
        } catch (RepositoryException e) {
			// Roll back session state after failure to keep next write clean
            discardPendingChanges();
            return WriteResult.failure(record.rowNumber(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @Override
    public void clearTargetFolder() {
        String folderPath = config.target().jcrPath();
        try {
            if (!session.nodeExists(folderPath)) {
                throw new IllegalStateException("Zielordner existiert nicht: " + folderPath);
            }
            Node folderNode = session.getNode(folderPath);
            List<String> childNames = new ArrayList<>();

            NodeIterator iterator = folderNode.getNodes();
            while (iterator.hasNext()) {
                childNames.add(iterator.nextNode().getName());
            }

            for (String childName : childNames) {
                folderNode.getNode(childName).remove();
            }

            session.save();
        } catch (RepositoryException e) {
            discardPendingChanges();
            throw new IllegalStateException(
                    "Konnte Zielordner '" + folderPath + "' nicht leeren", e);
        }
    }

    @Override
    public boolean exists(MappedRecord record) {
        if (matchByTargetProperty == null) {
            return false;
        }
        Object expected = record.properties().get(matchByTargetProperty);
        if (expected == null) {
            return false;
        }
        String expectedString = expected.toString();
        String folderPath = config.target().jcrPath();
        try {
            if (!session.nodeExists(folderPath)) {
                return false;
            }
            Node folderNode = session.getNode(folderPath);
			// Linear scan over folder children (acceptable for small datasets)
            NodeIterator nodes = folderNode.getNodes();

            while (nodes.hasNext()) {

                Node variant = nodes.nextNode();

                if (variant.hasProperty(matchByTargetProperty)
                        && expectedString.equals(
                        variant.getProperty(matchByTargetProperty).getString())) {
                    return true;
                }
            }
            return false;
        } catch (RepositoryException e) {
			// Return false on repository errors (safe fallback, avoids breaking import flow)
            return false;
        }
    }

    // --- initialization helpers ----------------------------------------------

    /**
     * Map source matchBy field to target JCR property name
     */
    private static String resolveMatchByTargetProperty(MappingConfig config) {
        String matchBySource = config.strategy().matchBy();
        if (matchBySource == null) {
            return null;
        }
        for (ColumnMapping column : config.columns()) {
            if (matchBySource.equals(column.sourceName())) {
                return column.targetProperty();
            }
        }
        throw new IllegalStateException(
                "matchBy-Spalte '" + matchBySource + "' nicht in columns gefunden");
    }

    // --- Names ----------------------------------------------------

    private String computeNodeName(MappedRecord record) {
        if (matchByTargetProperty == null) {
            return randomFallbackName();
        }
        Object value = record.properties().get(matchByTargetProperty);
        if (value == null) {
            return randomFallbackName();
        }
        return toValidNodeName(value.toString());
    }

    private String computeDisplayName(MappedRecord record, String fallback) {
        if (matchByTargetProperty == null) {
            return fallback;
        }
        Object value = record.properties().get(matchByTargetProperty);
        return value == null ? fallback : value.toString();
    }

    private static String randomFallbackName() {
        return "doc-" + UUID.randomUUID();
    }

    /**
     * Normalize string to JCR-safe node name (lowercase, alphanumeric + '-')
     */
    private static String toValidNodeName(String raw) {
        String sanitized = raw.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return sanitized.isEmpty() ? randomFallbackName() : sanitized;
    }

    // --- Property-Writes -------------------------------------------------

    private static void setProperties(Node variant, Map<String, Object> properties) throws RepositoryException {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
			// Typed values bypass string conversion so JCR stores proper types.
            if (value instanceof Double d) {
                variant.setProperty(entry.getKey(), d);
            } else if (value instanceof Boolean b) {
                variant.setProperty(entry.getKey(), b);
            } else {
                setProperty(variant, entry.getKey(), value.toString());
            }
        }
    }

    /**
     * Heuristic typing: boolean strings become boolean, else string.
     * Empty source cells are skipped earlier in MappingService — JCR
     * defaults from the doctype prototype (e.g. hasLocalShop=false)
     * cover that case automatically.
     */
    private static void setProperty(Node variant, String name, String stringValue) throws RepositoryException {
        if ("true".equalsIgnoreCase(stringValue) || "x".equalsIgnoreCase(stringValue)) {
            variant.setProperty(name, true);
        } else if ("false".equalsIgnoreCase(stringValue)) {
            variant.setProperty(name, false);
        } else {
            variant.setProperty(name, stringValue);
        }
    }

    // --- Workflow- / Session- Helpers --------------------------------------

    private void discardPendingChanges() {
        try {
            session.refresh(false);
        } catch (RepositoryException ignored) {
			// Reset session state without saving (error recovery)
        }
    }
}
