package org.mycompany.mapping;

import org.mycompany.config.MappingConfig;
import org.mycompany.config.MappingConfig.ColumnMapping;
import org.mycompany.model.MappedRecord;
import org.mycompany.model.Record;
import org.mycompany.model.ValidationFailure;
import org.mycompany.model.ValidationResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps and validates a record based on MappingConfig.
 * All validation logic is delegated to Validators.
 */
public class MappingService {

	private final MappingConfig config;
	private final Map<String, Validator> validatorRegistry;
	private final Map<String, Set<String>> seenValues = new HashMap<>();

	private boolean isFirstOccurrence(String field, String value) {
		if (value == null) { return false; }

		return seenValues
			.computeIfAbsent(field, k -> new HashSet<>())
			.add(value);
	}

	public MappingService(MappingConfig config, List<Validator> validators) {
		this.config = config;

		this.validatorRegistry = validators.stream()
			.collect(Collectors.toUnmodifiableMap(Validator::name, v -> v));

		verifyValidatorsExist();
	}

	public Result map(Record record) {
		List<ValidationFailure> failures = new ArrayList<>();
		Map<String, Object> properties = new LinkedHashMap<>();

		for (ColumnMapping column : config.columns()) {
			String raw = record.get(column.sourceName()).orElse(null);
			String value = normalize(raw);

			boolean fieldHasError = false;

			// validator existence check (multi-validator aware)
			for (String v : column.validators()) {
				if (!validatorRegistry.containsKey(v)) {
					throw new IllegalStateException("Unknown validator: " + v);
				}
			}

			// 1. validators (multi support)
			for (String v : column.validators()) {
				Validator validator = validatorRegistry.get(v);

				Optional<ValidationFailure> failure =
					validator.validate(column.sourceName(), value);

				if (failure.isPresent()) {
					failures.add(failure.get());
					fieldHasError = true; // only set on REAL failure
				}
			}

			// 2. only map valid data (unchanged logic, but now correct)
			if (!fieldHasError) {
				properties.put(column.targetProperty(), value);

				if (column.firstOccurrenceFlag() != null) {
					boolean firstOccurrence =
						isFirstOccurrence(column.sourceName(), value);

					properties.put(
						column.firstOccurrenceFlag(),
						firstOccurrence
					);
				}
			}
		}

		if (!failures.isEmpty()) {
			return new Rejected(ValidationResult.invalid(record.rowNumber(), failures));
		}

		return new Mapped(new MappedRecord(
			record.rowNumber(),
			properties));
	}

	// --- helpers --------------------------------------------------------
	private void verifyValidatorsExist() {
		for (ColumnMapping column : config.columns()) {
			for (String v : column.validators()) {
				if (!validatorRegistry.containsKey(v)) {
					throw new IllegalStateException(
						"Unknown validator: " + v + "' for column '"
							+ column.sourceName() + "'. Configured validators: "
							+ validatorRegistry.keySet()
					);
				}
			}
		}
	}

	private static String normalize(String raw) {
		if (raw == null) return null;
		String t = raw.trim();
		return t.isEmpty() ? null : t;
	}

	// --- result ---------------------------------------------------------

	public sealed interface Result permits Mapped, Rejected {}

	public record Mapped(MappedRecord record) implements Result {}

	public record Rejected(ValidationResult validationResult) implements Result {}
}
