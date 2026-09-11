package org.mycompany.persistence;

import org.junit.jupiter.api.Test;
import org.mycompany.model.MappedRecord;
import org.mycompany.model.Record;
import org.mycompany.model.WriteResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SqliteImportRepositoryTest {

    @Test
    void shouldWriteRecordToDatabase() {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:target/test-import.db");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS wheretobuy (
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

        SqliteImportRepository repository =
                new SqliteImportRepository(jdbcTemplate);

        MappedRecord record = new MappedRecord(
                1,
                Map.ofEntries(
                        Map.entry("name", "Test Shop"),
                        Map.entry("group", "Test Group"),
                        Map.entry("address", "Teststraße 1"),
                        Map.entry("city", "Hamburg"),
                        Map.entry("zipcode", "20095"),
                        Map.entry("phone", "040123456"),
                        Map.entry("email", "test@example.com"),
                        Map.entry("url", "https://example.com"),
                        Map.entry("hasOnlineShop", true),
                        Map.entry("hasLocalShop", false),
                        Map.entry("autocompleteName", "Test Shop"),
                        Map.entry("latitude", 53.5511),
                        Map.entry("longitude", 9.9937)
                )
        );
        WriteResult result = repository.write(record);

        System.out.println("Error: " + result.error());

        assertTrue(result.isSuccess(), result.error());

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wheretobuy",
                Integer.class
        );

        assertEquals(1, count);

        String name = jdbcTemplate.queryForObject(
                "SELECT name FROM wheretobuy",
                String.class
        );

        assertEquals("Test Shop", name);
    }
}
