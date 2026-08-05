package org.mycompany.config;

import org.junit.jupiter.api.Test;
import org.mycompany.config.ConfigLoader;
import org.mycompany.config.MappingConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic ConfigLoader tests.
 *
 * Covers:
 *  - loading YAML from classpath
 *  - @JsonProperty mapping for reserved keywords
 *  - column and enrichment parsing
 *  - enum mapping
 *  - unknown config handling
 *
 * Not covered:
 *  - semantic config validation
 */
class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    void loadsWheretobuyConfigFromClasspath() {
        MappingConfig config = loader.load("wheretobuy");

        assertEquals("wheretobuy", config.type());
        assertNotNull(config.target());
        assertEquals("ht:wheretobuydocument", config.target().nodeType());
        assertEquals("/content/documents/wheretobuy",
                     config.target().jcrPath());
    }

    @Test
    void parsesStrategyAndRenamesDefaultKeyword() {
        MappingConfig config = loader.load("wheretobuy");

		// Verifies @JsonProperty("default") -> defaultStrategy mapping.
        assertEquals("replaceFolder", config.strategy().defaultStrategy());
        assertEquals("shopname",      config.strategy().matchBy());
    }

    @Test
    void parsesAllColumnMappings() {
        MappingConfig config = loader.load("wheretobuy");

        assertEquals(15, config.columns().size());

        MappingConfig.ColumnMapping first = config.columns().get(0);
        assertEquals("shopname", first.sourceName());
        assertEquals("ht:name",  first.targetProperty());
		assertNotNull(first.validators());
		assertTrue(first.validators().contains("required"));
    }

    @Test
    void parsesEnrichmentAndOnFailureEnum() {
        MappingConfig config = loader.load("wheretobuy");

        assertEquals(1, config.enrichment().size());
        MappingConfig.EnrichmentRule rule = config.enrichment().get(0);

        assertEquals("geocoding", rule.service());
        assertEquals(3, rule.sourceFields().size());
		// Verifies enum mapping from YAML string.
        assertEquals(MappingConfig.OnFailure.CONTINUE_WITHOUT_ENRICHMENT,
                     rule.onFailure());
    }

    @Test
    void throwsWhenConfigForUnknownTypeIsRequested() {
		// Should fail for missing config files.
        assertThrows(IllegalArgumentException.class,
                     () -> loader.load("does-not-exist"));
    }
}
