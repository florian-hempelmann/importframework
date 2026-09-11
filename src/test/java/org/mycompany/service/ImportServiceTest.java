package org.mycompany.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.mycompany.config.ConfigLoader;
import org.mycompany.mapping.EmailValidator;
import org.mycompany.mapping.RequiredValidator;
import org.mycompany.model.ImportReport;
import org.mycompany.parser.ExcelParser;
import org.mycompany.parser.ParserFactory;
import org.mycompany.persistence.ImportRepository;
import org.mycompany.persistence.SqliteImportRepository;
import org.mycompany.strategy.ReplaceFolderStrategy;
import org.mycompany.strategy.UpdateStrategy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldImportValidExcel() throws IOException {
        Path database = tempDir.resolve("valid-import.db");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + database);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        createSchema(jdbcTemplate);

        ImportRepository repository =
                new SqliteImportRepository(jdbcTemplate);

        ConfigLoader configLoader = new ConfigLoader();

        ParserFactory parserFactory =
                new ParserFactory(List.of(new ExcelParser()));

        List<org.mycompany.mapping.Validator> validators = List.of(
                new RequiredValidator(),
                new EmailValidator()
        );

        List<UpdateStrategy> strategies = List.of(
                new ReplaceFolderStrategy()
        );

        ImportService importService = new ImportService(
                configLoader,
                parserFactory,
                validators,
                strategies,
                null,
                repository
        );

        try (InputStream input = getClass()
                .getResourceAsStream("/wheretobuy/valid.xlsx")) {

            assertNotNull(input, "Test file valid.xlsx was not found");

            ImportReport report = importService.runImport(
                    "wheretobuy-sqlite",
                    "valid.xlsx",
                    input,
                    "test"
            );

            assertEquals(0, report.totals().failed());
            assertTrue(report.totals().succeeded() > 0);
            assertFalse(report.hasFailures());

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM wheretobuy",
                    Integer.class
            );

            assertEquals(report.totals().succeeded(), count);
        }
    }

    @Test
    void shouldReportValidationErrorsForInvalidExcel() throws IOException {
        Path database = tempDir.resolve("invalid-import.db");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + database);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        createSchema(jdbcTemplate);

        ImportRepository repository =
                new SqliteImportRepository(jdbcTemplate);

        ConfigLoader configLoader = new ConfigLoader();

        ParserFactory parserFactory =
                new ParserFactory(List.of(new ExcelParser()));

        List<org.mycompany.mapping.Validator> validators = List.of(
                new RequiredValidator(),
                new EmailValidator()
        );

        List<UpdateStrategy> strategies = List.of(
                new ReplaceFolderStrategy()
        );

        ImportService importService = new ImportService(
                configLoader,
                parserFactory,
                validators,
                strategies,
                null,
                repository
        );

        try (InputStream input = getClass()
                .getResourceAsStream("/wheretobuy/invalid.xlsx")) {

            assertNotNull(input, "Test file invalid.xlsx was not found");

            ImportReport report = importService.runImport(
                    "wheretobuy-sqlite",
                    "invalid.xlsx",
                    input,
                    "test"
            );

            assertTrue(report.totals().failed() > 0);
            assertTrue(report.hasFailures());

            assertTrue(
                    report.failures().stream()
                            .anyMatch(f ->
                                    f.reason().toLowerCase().contains("required"))
            );
        }
    }

    @Test
    void shouldReplaceExistingRecordsWhenImportingValidExcel() throws IOException {
        Path database = tempDir.resolve("replace-folder-import.db");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:" + database);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        createSchema(jdbcTemplate);

        // Existing data that should be removed by replaceFolder.
        jdbcTemplate.update("""
            INSERT INTO wheretobuy (name, city)
            VALUES (?, ?)
            """,
                "Old Shop",
                "Old City"
        );

        Integer beforeImport = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wheretobuy",
                Integer.class
        );

        assertEquals(1, beforeImport);

        ImportRepository repository =
                new SqliteImportRepository(jdbcTemplate);

        ConfigLoader configLoader = new ConfigLoader();

        ParserFactory parserFactory =
                new ParserFactory(List.of(new ExcelParser()));

        List<org.mycompany.mapping.Validator> validators = List.of(
                new RequiredValidator(),
                new EmailValidator()
        );

        List<UpdateStrategy> strategies = List.of(
                new ReplaceFolderStrategy()
        );

        ImportService importService = new ImportService(
                configLoader,
                parserFactory,
                validators,
                strategies,
                null,
                repository
        );

        ImportReport report;

        try (InputStream input = getClass()
                .getResourceAsStream("/wheretobuy/valid.xlsx")) {

            assertNotNull(input, "Test file valid.xlsx was not found");

            report = importService.runImport(
                    "wheretobuy-sqlite",
                    "valid.xlsx",
                    input,
                    "test"
            );
        }

        assertEquals(0, report.totals().failed());
        assertTrue(report.totals().succeeded() > 0);

        Integer afterImport = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wheretobuy",
                Integer.class
        );

        assertEquals(report.totals().succeeded(), afterImport);

        Integer oldShopCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wheretobuy WHERE name = ?",
                Integer.class,
                "Old Shop"
        );

        assertEquals(0, oldShopCount);
    }

    private static void createSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
                CREATE TABLE wheretobuy (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    "group" TEXT,
                    address TEXT,
                    city TEXT,
                    zipcode TEXT,
                    phone TEXT,
                    fax TEXT,
                    email TEXT,
                    url TEXT,
                    onlineshopname TEXT,
                    has_online_shop BOOLEAN,
                    has_local_shop BOOLEAN,
                    autocomplete_name TEXT,
                    latitude REAL,
                    longitude REAL
                )
                """);
    }
}