//package org.mycompany.api;
//
//import org.mycompany.model.ImportReport;
//import org.mycompany.service.ImportService;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.ws.rs.Consumes;
//import jakarta.ws.rs.GET;
//import jakarta.ws.rs.POST;
//import jakarta.ws.rs.Path;
//import jakarta.ws.rs.PathParam;
//import jakarta.ws.rs.Produces;
//import jakarta.ws.rs.WebApplicationException;
//import jakarta.ws.rs.core.Context;
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.core.Response;
//
//import javax.jcr.RepositoryException;
//import javax.jcr.Session;
//import javax.jcr.SimpleCredentials;
//
//import org.apache.cxf.jaxrs.ext.multipart.Attachment;
//import org.apache.cxf.jaxrs.ext.multipart.Multipart;
//import org.onehippo.cms7.services.HippoServiceRegistry;
//import org.onehippo.repository.RepositoryService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.InputStream;
//import java.util.Base64;
//
///**
// * JAX-RS endpoint for import execution, registered by the
// * ImporterDaemonModule during Bloomreach startup.
// *
// * URL: POST /cms/ws/imports/{type}
// *
// * Authentication uses HTTP Basic Auth against the Hippo user system.
// * Each request opens its own JCR session because JCR sessions are
// * not thread-safe.
// */
//@Path("/imports")
//public class ImportController {
//
//    private static final Logger log = LoggerFactory.getLogger(ImportController.class);
//
//    private final ImportService importService;
//
//    public ImportController(ImportService importService) {
//        this.importService = importService;
//    }
//
//    @POST
//    @Path("/{type}")
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response startImport(
//            @PathParam("type") String type,
//            @Multipart("file") Attachment fileAttachment,
//            @Context HttpServletRequest request) {
//
//        Session session = null;
//        try {
//            session = login(request);
//			checkRole(session);
//
//            String filename = fileAttachment.getContentDisposition().getParameter("filename");
//			// No try-with-resources: CXF manages the multipart stream lifecycle.
//            InputStream content = fileAttachment.getObject(InputStream.class);
//
//            ImportReport report = importService.runImport(
//                    type, filename, content, session, session.getUserID());
//            return Response.ok(report).build();
//
//        } catch (WebApplicationException e) {
//			// login() already contains the HTTP response.
//            return e.getResponse();
//        } catch (IllegalArgumentException e) {
//			// Invalid import type or parser selection.
//            log.warn("Bad import request: {}", e.getMessage());
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity("error: " + e.getMessage()).build();
//        } catch (RuntimeException e) {
//            log.error("Import failed unexpectedly", e);
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                    .entity("error: " + e.getClass().getSimpleName() + ": " + e.getMessage())
//                    .build();
//        } catch (RepositoryException e) {
//			log.error("JCR repository error during import", e);
//			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//				.entity("error: " + e.getMessage())
//				.build();
//		} finally {
//            if (session != null && session.isLive()) {
//                session.logout();
//            }
//        }
//    }
//
//	/**
//	 * Returns the XLSX template for an import type from
//	 * /samples/{type}.xlsx inside the importer JAR.
//	 * Single canonical file used both as editor template
//	 * and test fixture to keep template and test data in sync.
//	 */
//    @GET
//    @Path("/{type}/sample")
//    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
//    public Response getSample(@PathParam("type") String type) {
//        InputStream sample = getClass().getResourceAsStream("/samples/" + type + ".xlsx");
//        if (sample == null) {
//            return Response.status(Response.Status.NOT_FOUND)
//                    .entity("no sample template for type: " + type).build();
//        }
//        return Response.ok(sample)
//                .header("Content-Disposition",
//                        "attachment; filename=\"" + type + "-template.xlsx\"")
//                .build();
//    }
//
//    private static Session login(HttpServletRequest request) {
//
//		// authorization-data to "user:password" (cms)
//		String authHeader = request.getHeader("Authorization");
//        if (authHeader == null || !authHeader.startsWith("Basic ")) {
//            throw unauthorized("authentication required");
//        }
//        String decoded = new String(Base64.getDecoder()
//                .decode(authHeader.substring("Basic ".length())));
//        int colon = decoded.indexOf(':');
//        if (colon < 0) {
//            throw unauthorized("malformed credentials");
//        }
//        String user = decoded.substring(0, colon);
//        String password = decoded.substring(colon + 1);
//
//		/**
//		 * get jcr session as the user, who requested
//		 * (hippostdpubwf:createdBy = <user-id>)
//		 * */
//        RepositoryService repo = HippoServiceRegistry.getService(RepositoryService.class);
//        if (repo == null) {
//            throw new WebApplicationException(
//                    Response.status(Response.Status.SERVICE_UNAVAILABLE)
//                            .entity("repository service not available").build());
//        }
//        try {
//            return repo.login(new SimpleCredentials(user, password.toCharArray()));
//        } catch (Exception e) {
//            throw unauthorized("invalid credentials");
//        }
//    }
//
//    private static WebApplicationException unauthorized(String message) {
//        return new WebApplicationException(
//                Response.status(Response.Status.UNAUTHORIZED)
//                        .header("WWW-Authenticate", "Basic realm=\"import-framework\"")
//                        .entity(message).build());
//    }
//
//	private static void checkRole(Session session) throws RepositoryException {
//		if (!session.hasPermission("/content", "jcr:write")) {
//			throw unauthorized("import not allowed for this user");
//		}
//	}
//}
