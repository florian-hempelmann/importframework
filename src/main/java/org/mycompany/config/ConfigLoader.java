package org.mycompany.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads a YAML mapping config for a given import type from the classpath.
 * Files are expected under /mappings/{type}.yaml.
 */
public class ConfigLoader {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public MappingConfig load(String type) {
        String resourcePath = "/mappings/" + type + ".yaml";

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException(
                        "Keine Mapping-Konfiguration für Importtyp '" + type + "' gefunden "
                                + "(erwartet unter " + resourcePath + ")");
            }
            return yamlMapper.readValue(is, MappingConfig.class);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Fehler beim Lesen der Mapping-Konfiguration für Importtyp '" + type + "'", e);
        }
    }
}
