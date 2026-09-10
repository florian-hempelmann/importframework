package org.mycompany.mapping;

import org.mycompany.config.ConfigLoader;
import org.mycompany.config.MappingConfig;
import org.mycompany.mapping.EmailValidator;
import org.mycompany.mapping.MappingService;
import org.mycompany.mapping.RequiredValidator;
import org.mycompany.model.MappedRecord;
import org.mycompany.model.Record;
import org.mycompany.model.ValidationFailure;
import org.mycompany.model.ValidationResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for mapping layer (spec 4.2.3, 4.7.3).
 *
 * Uses real wheretobuy-bloomreach.yaml config.
 * ConfigLoader is tested separately; here we test MappingService only.
 *
 * Validator list is empty: no external validators used.
 * required/unique are handled internally by the service.
 */
class MappingServiceTest {

	private MappingService service;

	@BeforeEach
	void setUp() {
		MappingConfig config = new ConfigLoader().load("wheretobuy-sqlite");
		service = new MappingService(config, List.of(
			new RequiredValidator(),
			new EmailValidator()
		));
	}

	@Test
	void mapsCompleteRecordToMapped() {
		Record record = recordWith(
			"shopname",       	"Test Shop",
			"federation",     	"Group A",
			"address",         	"Hauptstrasse 1",
			"city",           	"Berlin",
			"zipcode",        	"10115",
			"phone",    		"+49 30 123",
			"fax",      		"+49 30 124",
			"email",          	"shop@test.de",
			"website",        	"https://test.de",
			"onlineshopname", 	"Shop Online",
			"hasonlineshop",  	"true",
			"haslocalshop",   	"true",
			"autocompletename",	"Test"
		);

		MappingService.Mapped mapped = assertInstanceOf(
			MappingService.Mapped.class, service.map(record));
		MappedRecord result = mapped.record();

		assertEquals(1, result.rowNumber());
		assertEquals(17, result.properties().size());
		assertEquals("Test Shop", result.properties().get("name"));
		assertEquals("Berlin", result.properties().get("city"));
		assertEquals("https://test.de", result.properties().get("url"));
	}

	@Test
	void rejectsRecordMissingRequiredShopname() {
		Record record = recordWith(
			"address", "Hauptstrasse 1",
			"city",    "Berlin"
			// shopname missing
		);

		MappingService.Rejected rejected = assertInstanceOf(
			MappingService.Rejected.class, service.map(record));

		ValidationResult vr = rejected.validationResult();

		assertEquals(1, vr.failures().size());

		ValidationFailure f1 = vr.failures().get(0);
		assertEquals("shopname", f1.columnName());
		assertEquals("required", f1.validatorName());
	}

	@Test
	void collectsMultipleFailuresAcrossColumns() {
		// shopname missing → required failure; email malformed → email validator failure
		Record record = recordWith(
			"email", "not-an-email"
		);

		MappingService.Rejected rejected = assertInstanceOf(
			MappingService.Rejected.class, service.map(record));

		ValidationResult vr = rejected.validationResult();

		assertEquals(2, vr.failures().size());

		assertEquals("shopname", vr.failures().get(0).columnName());
		assertEquals("required", vr.failures().get(0).validatorName());

		assertEquals("email", vr.failures().get(1).columnName());
		assertEquals("email", vr.failures().get(1).validatorName());
	}

	// --- Helpers ---------------------------------------------------------

	private static Record recordWith(String... keyValuePairs) {
		if (keyValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("Brauche paare aus key/value");
		}
		Map<String, String> fields = new LinkedHashMap<>();
		for (int i = 0; i < keyValuePairs.length; i += 2) {
			fields.put(keyValuePairs[i], keyValuePairs[i + 1]);
		}
		return new Record(1, fields);
	}
}
