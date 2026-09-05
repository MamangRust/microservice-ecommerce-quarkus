package com.sanedge.common.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import com.sanedge.common.exception.ForbiddenException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.UnauthorizedException;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

class GrpcErrorMapperTest {

    @Test
    void mapsDomainExceptionsToContractStatuses() {
        assertStatus(new InvalidRequestException("invalid payload"), Status.INVALID_ARGUMENT);
        assertStatus(new ResourceNotFoundException("missing resource"), Status.NOT_FOUND);
        assertStatus(new ResourceAlreadyExistsException("duplicate resource"), Status.ALREADY_EXISTS);
        assertStatus(new UnauthorizedException("missing credentials"), Status.UNAUTHENTICATED);
        assertStatus(new ForbiddenException("forbidden action"), Status.PERMISSION_DENIED);
    }

    @Test
    void preservesExistingGrpcStatus() {
        StatusRuntimeException original = Status.UNAVAILABLE.withDescription("dependency down").asRuntimeException();

        StatusRuntimeException mapped = GrpcErrorMapper.toStatusRuntimeException(original);

        assertThat(mapped).isSameAs(original);
    }

    @Test
    void preservesStatusExceptionCodeAndTrailers() {
        Metadata trailers = new Metadata();
        StatusException original = new StatusException(Status.DEADLINE_EXCEEDED.withDescription("timed out"), trailers);

        StatusRuntimeException mapped = GrpcErrorMapper.toStatusRuntimeException(original);

        assertThat(mapped.getStatus().getCode()).isEqualTo(Status.DEADLINE_EXCEEDED.getCode());
        assertThat(mapped.getStatus().getDescription()).isEqualTo("timed out");
        assertThat(mapped.getTrailers()).isSameAs(trailers);
    }

    @Test
    void unwrapsCompletionExceptionBeforeMapping() {
        StatusRuntimeException mapped = GrpcErrorMapper.toStatusRuntimeException(
                new CompletionException(new ResourceNotFoundException("missing")));

        assertThat(mapped.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        assertThat(mapped.getStatus().getDescription()).isEqualTo("missing");
    }

    @Test
    void mapsUnknownFailuresToInternal() {
        StatusRuntimeException mapped = GrpcErrorMapper.toStatusRuntimeException(new IllegalStateException("boom"));

        assertThat(mapped.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
        assertThat(mapped.getStatus().getDescription()).isEqualTo("boom");
    }

    @Test
    void usesStatusNameWhenFailureMessageIsBlank() {
        StatusRuntimeException mapped = GrpcErrorMapper.toStatusRuntimeException(new RuntimeException(" "));

        assertThat(mapped.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
        assertThat(mapped.getStatus().getDescription()).isEqualTo(Status.INTERNAL.getCode().name());
    }

    private static void assertStatus(Throwable failure, Status expected) {
        StatusRuntimeException mapped = GrpcErrorMapper.toStatusRuntimeException(failure);

        assertThat(mapped.getStatus().getCode()).isEqualTo(expected.getCode());
    }
}
