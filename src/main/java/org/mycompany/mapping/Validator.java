package org.mycompany.mapping;

import org.mycompany.model.ValidationFailure;

import java.util.Optional;

/**
 * Generic validation rule applied per cell.
 * Stateless by design. State (if needed) is handled via ValidationContext.
 * Used via registry lookup in MappingService.
 */
public interface Validator {

	/**
	 * Name used in YAML + ValidationFailure (e.g. "required")
	 */
	String name();

	Optional<ValidationFailure> validate(
		String columnName, String value);
}
