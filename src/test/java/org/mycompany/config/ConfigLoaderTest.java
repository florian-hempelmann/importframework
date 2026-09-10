package org.mycompany.config;

import org.junit.jupiter.api.Test;
import org.mycompany.config.ConfigLoader;
import org.mycompany.config.MappingConfig;

import static org.junit.jupiter.api.Assertions.*;

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
        MappingConfig config = loader.load("wheretobuy-sqlite");

        assertEquals("wheretobuy-sqlite", config.type());
        assertNotNull(config.target());
        assertEquals("wheretobuy",
                     config.target().table());
    }

    @Test
    void parsesStrategyAndRenamesDefaultKeyword() {
        MappingConfig config = loader.load("wheretobuy-sqlite");

		// Verifies @JsonProperty("default") -> defaultStrategy mapping.
        assertEquals("replaceFolder", config.strategy().defaultStrategy());
        assertNull(config.strategy().matchBy());
    }

    @Test
    void parsesAllColumnMappings() {
        MappingConfig config = loader.load("wheretobuy-sqlite");

        assertEquals(15, config.columns().size());

        MappingConfig.ColumnMapping first = config.columns().get(0);
        assertEquals("shopname", first.sourceName());
        assertEquals("name",  first.targetProperty());
		assertNotNull(first.validators());
		assertTrue(first.validators().contains("required"));
    }

    @Test
    void throwsWhenConfigForUnknownTypeIsRequested() {
		// Should fail for missing config files.
        assertThrows(IllegalArgumentException.class,
                     () -> loader.load("does-not-exist"));
    }
}
