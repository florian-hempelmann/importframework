//package org.mycompany.security;
//
//import jakarta.ws.rs.container.ContainerRequestFilter;
//import jakarta.ws.rs.container.ContainerRequestContext;
//import jakarta.ws.rs.core.Response;
//import jakarta.ws.rs.ext.Provider;
//
//@Provider
//public class UploadSizeLimitFilter implements ContainerRequestFilter {
//
//	private static final long MAX_SIZE = 50L * 1024L * 1024L;
//
//	@Override
//	public void filter(ContainerRequestContext requestContext) {
//		long len = requestContext.getLength();
//
//		if (len > MAX_SIZE) {
//			requestContext.abortWith(
//				Response.status(413)
//					.entity("File too large (max 50MB)")
//					.build()
//			);
//		}
//	}
//}
