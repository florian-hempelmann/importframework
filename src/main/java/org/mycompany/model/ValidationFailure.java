package org.mycompany.model;

/**
 * A single validation error.
 * columnName    — source column where the error occurred
 * validatorName — rule name ("required", "lat-lon", "countryCode", …)
 * reason        — human-readable error message for the import report
 */
public record ValidationFailure(String columnName, String validatorName, String reason) { }
