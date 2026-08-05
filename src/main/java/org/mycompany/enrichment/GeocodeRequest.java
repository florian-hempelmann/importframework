package org.mycompany.enrichment;

/**
 * Address payload for a geocoding lookup.
 * All fields are optional; the provider concatenates available parts.
 */
public record GeocodeRequest(String street, String zipcode, String city, String country) { }