package com.sanedge.gateway.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.sanedge.common.domain.response.ApiResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.ws.rs.core.Response;

class GrpcExceptionMapperTest {

    private final GrpcExceptionMapper mapper = new GrpcExceptionMapper();

    @Test
    void mapsClientAndBusinessStatusesToHttpContract() {
        assertHttpStatus(Status.INVALID_ARGUMENT, 400);
        assertHttpStatus(Status.UNAUTHENTICATED, 401);
        assertHttpStatus(Status.PERMISSION_DENIED, 403);
        assertHttpStatus(Status.NOT_FOUND, 404);
        assertHttpStatus(Status.ALREADY_EXISTS, 409);
        assertHttpStatus(Status.FAILED_PRECONDITION, 422);
        assertHttpStatus(Status.RESOURCE_EXHAUSTED, 429);
    }

    @Test
    void mapsDependencyStatusesToHttpContract() {
        assertHttpStatus(Status.UNAVAILABLE, 503);
        assertHttpStatus(Status.DEADLINE_EXCEEDED, 504);
        assertHttpStatus(Status.INTERNAL, 500);
    }

    @Test
    void usesSafeFallbackMessageWhenGrpcDescriptionIsMissing() {
        try (Response response = mapper.toResponse(Status.NOT_FOUND.asRuntimeException())) {
            assertEquals(404, response.getStatus());
            ApiResponse<?> body = (ApiResponse<?>) response.getEntity();
            assertNotNull(body);
            assertEquals("NOT_FOUND", body.message());
        }
    }

    private void assertHttpStatus(Status status, int expectedHttpStatus) {
        try (Response response = mapper.toResponse(status.withDescription("failure").asRuntimeException())) {
            assertEquals(expectedHttpStatus, response.getStatus(), status.getCode().name());
            ApiResponse<?> body = (ApiResponse<?>) response.getEntity();
            assertNotNull(body);
            assertEquals("failure", body.message());
        }
    }
}
