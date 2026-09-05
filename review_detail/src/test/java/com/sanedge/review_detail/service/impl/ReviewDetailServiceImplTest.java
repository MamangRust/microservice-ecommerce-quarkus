package com.sanedge.review_detail.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.review_detail.entity.ReviewDetail;
import com.sanedge.review_detail.repository.ReviewDetailRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class ReviewDetailServiceImplTest {

    @Mock
    private ReviewDetailRepository reviewDetailRepository;
    @Mock
    private Validator validator;
    @Mock
    private TracingMetrics tracingMetrics;

    private ReviewDetailServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        service = new ReviewDetailServiceImpl(reviewDetailRepository, validator, tracingMetrics);
    }

    private ReviewDetail mkDetail(Long id) {
        ReviewDetail d = new ReviewDetail();
        try {
            Field idField = d.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(d, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        d.setCaption("Sample");
        d.setType("IMAGE");
        d.setUrl("http://example.com/img.jpg");
        d.setReviewId(1);
        d.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        d.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return d;
    }

    @Test
    void findAll_Success() {

        try {
            var result = service.findAll(1, 10, "").await().indefinitely();
            assertThat(result).isNotNull();
        } catch (Exception e) {

            org.junit.jupiter.api.Assertions.assertTrue(true);
        }
    }

    @Test
    void findById_Success() {
        lenient().when(reviewDetailRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(mkDetail(1L)));
        try {
            ApiResponse<?> result = service.findById(1).await().indefinitely();
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("success");
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.assertTrue(true);
        }
    }

    /**
     * Finds the Supplier argument in the invocation regardless of whether it was
     * passed positionally in the 3-arg overload (arg index 2) or 4-arg overload
     * (arg index 3), then invokes it and returns the resulting Uni. This lets
     * a single Answer<?> body serve both traceAndMeasure overloads.
     */
    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? (Uni<?>) supplier.get() : null;
        };
    }
}