package com.sanedge.gateway.filter;

import com.sanedge.common.domain.response.ApiResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import io.quarkus.logging.Log;

@Provider
public class ErrorHandlingFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object entity = responseContext.getEntity();
        if (entity == null) {
            return;
        }

        try {
            Method statusMethod = null;
            try {
                statusMethod = entity.getClass().getMethod("status");
            } catch (NoSuchMethodException e) {
                try {
                    statusMethod = entity.getClass().getMethod("getStatus");
                } catch (NoSuchMethodException ex) {
                }
            }

            if (statusMethod != null) {
                String status = (String) statusMethod.invoke(entity);
                if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                    String message = "Operation failed";
                    Method messageMethod = null;
                    try {
                        messageMethod = entity.getClass().getMethod("message");
                    } catch (NoSuchMethodException e) {
                        try {
                            messageMethod = entity.getClass().getMethod("getMessage");
                        } catch (NoSuchMethodException ex) {
                            // No message method found on the object
                        }
                    }

                    if (messageMethod != null) {
                        message = (String) messageMethod.invoke(entity);
                    }

                    int httpStatus = determineHttpStatus(message);
                    Log.warnf("Payload indicated failure: status=%s, message='%s'. Overriding HTTP status to %d.",
                            status, message, httpStatus);

                    responseContext.setStatus(httpStatus);
                    responseContext.setEntity(ApiResponse.error(message));
                    responseContext.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_JSON);
                }
            }
        } catch (Exception e) {
            Log.error("Error occurred while processing response error handling filter", e);
        }
    }

    private int determineHttpStatus(String message) {
        if (message == null) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
        String msgLower = message.toLowerCase();
        if (msgLower.contains("not found")) {
            return Response.Status.NOT_FOUND.getStatusCode();
        } else if (msgLower.contains("already exists") || msgLower.contains("already registered")) {
            return Response.Status.CONFLICT.getStatusCode();
        } else if (msgLower.contains("unauthorized") || msgLower.contains("credentials") || msgLower.contains("expired")
                || msgLower.contains("wrong password") || msgLower.contains("invalid credential")) {
            return Response.Status.UNAUTHORIZED.getStatusCode();
        } else if (msgLower.contains("forbidden") || msgLower.contains("denied")) {
            return Response.Status.FORBIDDEN.getStatusCode();
        } else {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
    }
}
