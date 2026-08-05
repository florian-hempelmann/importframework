package org.mycompany.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Google Maps Geocoding API implementation.
 * Sends URL-encoded GET requests to the public API.
 * API key is provided via constructor injection.
 */
public class GoogleMapsGeocodingService implements GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GoogleMapsGeocodingService.class);

    private static final String ENDPOINT = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	// Legacy country-code mappings for Google Maps hints.
    private static final Map<String, String> COUNTRY_EXPANSIONS = Map.of(
            "sv", "Sweden",
            "da", "Denmark",
            "fr", "France",
            "fi", "Finland",
            "es", "Spain",
            "za", "South Africa");

    private final HttpClient httpClient;
    private final ObjectMapper jsonMapper;
    private final String apiKey;

    public GoogleMapsGeocodingService(String apiKey) {
        this(apiKey, HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build(), new ObjectMapper());
    }

    GoogleMapsGeocodingService(String apiKey, HttpClient httpClient, ObjectMapper jsonMapper) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Google Maps API key must not be blank");
        }
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Coordinates geocode(GeocodeRequest request) {
        String addressParam = buildAddressParam(request);
        URI uri = URI.create(ENDPOINT + "?address=" + addressParam + "&key=" + apiKey);

        HttpResponse<String> response = sendRequest(uri);
        if (response.statusCode() != 200) {
            throw new GeocodingException(
                    "Google Maps returned HTTP " + response.statusCode());
        }
        return parseResponse(response.body());
    }

    private HttpResponse<String> sendRequest(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new GeocodingException("Geocoding request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeocodingException("Geocoding request interrupted", e);
        }
    }

    private Coordinates parseResponse(String body) {
        JsonNode root;
        try {
            root = jsonMapper.readTree(body);
        } catch (java.io.IOException e) {
            throw new GeocodingException("Malformed JSON from Google Maps", e);
        }

        String status = root.path("status").asText("");
        if (!"OK".equals(status)) {
            throw new GeocodingException("Google Maps status=" + status);
        }
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            throw new GeocodingException("Google Maps returned empty results");
        }
        JsonNode location = results.get(0).path("geometry").path("location");
        if (!location.has("lat") || !location.has("lng")) {
            throw new GeocodingException("Missing lat/lng in Google Maps response");
        }
        return new Coordinates(location.get("lat").asDouble(), location.get("lng").asDouble());
    }

    private static String buildAddressParam(GeocodeRequest request) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, request.street());
        appendPart(sb, joinNonBlank(request.zipcode(), request.city()));
        appendPart(sb, expandCountry(request.country()));
        return URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(part.trim());
    }

    private static String joinNonBlank(String a, String b) {
        boolean hasA = a != null && !a.isBlank();
        boolean hasB = b != null && !b.isBlank();
        if (hasA && hasB) {
            return a.trim() + " " + b.trim();
        }
        return hasA ? a.trim() : (hasB ? b.trim() : null);
    }

    private static String expandCountry(String country) {
        if (country == null || country.isBlank()) {
            return null;
        }
        return COUNTRY_EXPANSIONS.getOrDefault(country.toLowerCase(), country);
    }
}
