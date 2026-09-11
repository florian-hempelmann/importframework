package org.mycompany.mapping;

import org.mycompany.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Required field validation.
 */

@Component
public class RequiredValidator implements Validator {

	@Override
	public String name() {
		return "required";
	}

	@Override
	public Optional<ValidationFailure> validate(
		String columnName,
		String value
	) {
		if (value == null || value.isBlank()) {
			return Optional.of(new ValidationFailure(columnName,
					name(), "Required field missing.")
			);
		}
		return Optional.empty();
	}
}
