package org.mycompany.enrichment;

/**
 * Thrown when a geocoding lookup fails (network error, non-OK API status,
 * malformed response, zero results).
 */
public class GeocodingException extends RuntimeException {

    public GeocodingException(String message) {
        super(message);
    }

    public GeocodingException(String message, Throwable cause) {
        super(message, cause);
    }
}