package com.sanedge.common.grpc;

import com.sanedge.common.exception.ForbiddenException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.UnauthorizedException;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Converts domain failures into the gRPC status contract used by all services.
 *
 * <p>The mapper deliberately keeps unknown failures as INTERNAL while preserving
 * an existing gRPC failure. This prevents handlers from accidentally replacing
 * a meaningful status with INTERNAL and keeps infrastructure failures opaque to
 * clients.</p>
 */
public final class GrpcErrorMapper {

    private GrpcErrorMapper() {
    }

    public static StatusRuntimeException toStatusRuntimeException(Throwable failure) {
        Throwable cause = unwrap(failure);

        if (cause instanceof StatusRuntimeException statusFailure) {
            return statusFailure;
        }
        if (cause instanceof StatusException statusFailure) {
            return new StatusRuntimeException(statusFailure.getStatus(), statusFailure.getTrailers());
        }
        if (cause instanceof ResourceNotFoundException) {
            return status(Status.NOT_FOUND, cause);
        }
        if (cause instanceof ResourceAlreadyExistsException) {
            return status(Status.ALREADY_EXISTS, cause);
        }
        if (cause instanceof InvalidRequestException) {
            return status(Status.INVALID_ARGUMENT, cause);
        }
        if (cause instanceof UnauthorizedException) {
            return status(Status.UNAUTHENTICATED, cause);
        }
        if (cause instanceof ForbiddenException) {
            return status(Status.PERMISSION_DENIED, cause);
        }

        return status(Status.INTERNAL, cause);
    }

    private static StatusRuntimeException status(Status status, Throwable failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            message = status.getCode().name();
        }
        return status.withDescription(message).asRuntimeException();
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
