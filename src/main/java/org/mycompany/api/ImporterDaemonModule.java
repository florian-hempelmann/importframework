//package org.mycompany.api;
//
//// Whole importer pipeline
//import org.mycompany.config.ConfigLoader;
//import org.mycompany.enrichment.GeocodingService;
//import org.mycompany.enrichment.GoogleMapsGeocodingService;
//import org.mycompany.mapping.EmailValidator;
//import org.mycompany.mapping.LatLonValidator;
//import org.mycompany.mapping.RequiredValidator;
//import org.mycompany.mapping.Validator;
//import org.mycompany.parser.ExcelParser;
//import org.mycompany.parser.Parser;
//import org.mycompany.parser.ParserFactory;
//import org.mycompany.security.UploadSizeLimitFilter;
//import org.mycompany.service.ImportService;
//import org.mycompany.strategy.ReplaceFolderStrategy;
//import org.mycompany.strategy.UpdateByIdStrategy;
//import org.mycompany.strategy.UpdateStrategy;
//
//import javax.jcr.RepositoryException;
//import javax.jcr.Session;
//import javax.naming.Context;
//import javax.naming.InitialContext;
//import javax.naming.NamingException;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
//
//import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
//import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
//import org.onehippo.repository.modules.DaemonModule; // important for bloomreach startup
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.List;
//
///**
// * DaemonModule initialized on Bloomreach startup.
// * Registers the JAX-RS ImportController at the RepositoryJaxrsServlet.
// *
// * Configured via HCM YAML:
// * repository-data/.../modules/import-framework.yaml
// *
// * Lifecycle:
// * - initialize(): builds ImportService and registers REST endpoint
// * - shutdown(): removes endpoint
// *
// * Important: the system Session here is only for bootstrapping.
// * Each HTTP request uses its own user Session (see controller).
// */
//public class ImporterDaemonModule implements DaemonModule {
//
//    private static final Logger log = LoggerFactory.getLogger(ImporterDaemonModule.class);
//
//	// Base path under /cms/ws.
//	// "/" avoids duplication with @Path("/imports") in controller:
//	// /cms/ws + "/" + "/imports" + "/{type}" => /cms/ws/imports/{type}
//    private static final String ENDPOINT_ADDRESS = "/";
//
//	// JNDI key for Google Maps Geocoding API.
//	// If missing, enrichment is skipped.
//	private static final String GEOCODING_KEY_JNDI = "java:comp/env/geocoding/googleMapsApiKey";
//
//    @Override
//    public void initialize(Session session) throws RepositoryException {
//        ImportService service = buildImportService();
//
//		// Register JAX-RS endpoint + JSON provider (Jackson config required
//		// for Java Records + JavaTimeModule support)
//		RepositoryJaxrsEndpoint endpoint =
//			new RepositoryJaxrsEndpoint(ENDPOINT_ADDRESS)
//				.singleton(new ImportController(service))
//				.singleton(buildJsonProvider())
//				.singleton(new UploadSizeLimitFilter());
//
//		RepositoryJaxrsService.addEndpoint(endpoint);
//
//        log.info("Import endpoints are registrated in /cms/ws/imports/* (POST /{type}, GET /{type}/sample)");
//    }
//
//    @Override
//    public void shutdown() {
//        RepositoryJaxrsService.removeEndpoint(ENDPOINT_ADDRESS);
//        log.info("Import-Endpoint deregistriert");
//    }
//
//    private static JacksonJsonProvider buildJsonProvider() {
//        ObjectMapper mapper = new ObjectMapper()
//                .registerModule(new JavaTimeModule())
//                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        return new JacksonJsonProvider(mapper);
//    }
//
//    private static ImportService buildImportService() {
//        List<Parser> parsers = List.of(new ExcelParser());
//        List<Validator> validators = List.of(new RequiredValidator(), new LatLonValidator(), new EmailValidator());
//        List<UpdateStrategy> strategies = List.of(
//                new ReplaceFolderStrategy(),
//                new UpdateByIdStrategy());
//
//        return new ImportService(
//                new ConfigLoader(),
//                new ParserFactory(parsers),
//                validators,
//                strategies,
//                buildGeocodingService());
//    }
//
//    private static GeocodingService buildGeocodingService() {
//        String apiKey = lookupApiKey();
//        if (apiKey == null || apiKey.isBlank()) {
//            log.warn("Geocoding API key not configured ({}); enrichment disabled",
//                    GEOCODING_KEY_JNDI);
//            return null;
//        }
//        log.info("Geocoding enabled via Google Maps");
//        return new GoogleMapsGeocodingService(apiKey);
//    }
//
//    private static String lookupApiKey() {
//        try {
//            Context ctx = new InitialContext();
//            return (String) ctx.lookup(GEOCODING_KEY_JNDI);
//        } catch (NamingException e) {
//            return null;
//        }
//    }
//}
