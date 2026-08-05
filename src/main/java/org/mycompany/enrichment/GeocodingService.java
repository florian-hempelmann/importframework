package org.mycompany.enrichment;

/**
 * Resolves an address to coordinates.
 * Implementations should be thread-safe.
 */
public interface GeocodingService {

	/**
	 * Returns coordinates for the given address.
	 * @throws GeocodingException on API or lookup failure
	 */
    Coordinates geocode(GeocodeRequest request);
}
