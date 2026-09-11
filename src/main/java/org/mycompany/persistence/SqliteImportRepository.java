package org.mycompany.persistence;

import org.mycompany.model.MappedRecord;
import org.mycompany.model.WriteResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class SqliteImportRepository implements ImportRepository {

    private final JdbcTemplate jdbcTemplate;

    public SqliteImportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public WriteResult write(MappedRecord record) {
        try {
            Map<String, Object> properties = record.properties();

            jdbcTemplate.update("""
                    INSERT INTO wheretobuy (
                        name,
                        "group",
                        address,
                        city,
                        zipcode,
                        phone,
                        fax,
                        email,
                        url,
                        onlineshopname,
                        has_online_shop,
                        has_local_shop,
                        autocomplete_name,
                        latitude,
                        longitude
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    properties.get("name"),
                    properties.get("group"),
                    properties.get("address"),
                    properties.get("city"),
                    properties.get("zipcode"),
                    properties.get("phone"),
                    properties.get("fax"),
                    properties.get("email"),
                    properties.get("url"),
                    properties.get("onlineshopname"),
                    properties.get("hasOnlineShop"),
                    properties.get("hasLocalShop"),
                    properties.get("autocompleteName"),
                    properties.get("latitude"),
                    properties.get("longitude")
            );

            return WriteResult.success(
                    record.rowNumber(),
                    "wheretobuy:" + record.rowNumber()
            );

        } catch (Exception e) {
            return WriteResult.failure(
                    record.rowNumber(),
                    e.getMessage()
            );
        }
    }

    @Override
    public void clearTargetFolder() {
        jdbcTemplate.update("DELETE FROM wheretobuy");
    }

    @Override
    public boolean exists(MappedRecord record) {
        // Wird für replaceFolder aktuell nicht benötigt.
        return false;
    }
}