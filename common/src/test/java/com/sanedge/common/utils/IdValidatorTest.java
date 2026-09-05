package com.sanedge.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;

class IdValidatorTest {

    @Test
    void invalidReturnsFailedUniMappedToInvalidArgument() {
        Uni<String> uni = IdValidator.invalid("Product id");

        String outcome = uni
                .onFailure().recoverWithItem(failure -> {
                    assertThat(failure).isInstanceOf(StatusRuntimeException.class);
                    StatusRuntimeException sre = (StatusRuntimeException) failure;
                    assertThat(sre.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
                    assertThat(sre.getStatus().getDescription()).isEqualTo("Product id must be a positive integer");
                    return "recovered";
                })
                .await().indefinitely();

        assertThat(outcome).isEqualTo("recovered");
    }

    @Test
    void invalidFailureMessageUsesProvidedFieldName() {
        Long outcome = IdValidator.<Long>invalid("Order id")
                .onFailure().recoverWithItem(failure -> {
                    assertThat(((StatusRuntimeException) failure).getStatus().getDescription())
                            .isEqualTo("Order id must be a positive integer");
                    return 0L;
                })
                .await().indefinitely();

        assertThat(outcome).isEqualTo(0L);
    }
}
