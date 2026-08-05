package org.mycompany.mapping;

import org.mycompany.model.ValidationFailure;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Simple email format validation.
 */
public class EmailValidator implements Validator {

	private static final Pattern EMAIL_PATTERN =
		Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

	@Override
	public String name() {
		return "email";
	}

	@Override
	public Optional<ValidationFailure> validate(
		String columnName,
		String value
	) {
		if (value == null || value.isBlank()) {
			return Optional.empty(); // required handles emptiness
		}

		if (!EMAIL_PATTERN.matcher(value).matches()) {
			return Optional.of(new ValidationFailure(columnName,
					name(),"'" + value + "' ist keine gültige E-Mail-Adresse")
			);
		}

		return Optional.empty();
	}
}
