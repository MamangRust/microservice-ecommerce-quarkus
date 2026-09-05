package com.sanedge.slider.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.slider.domain.requests.FindAllSliderRequest;
import com.sanedge.slider.entity.Slider;
import com.sanedge.slider.repository.SliderQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class SliderQueryServiceImplTest {

    @Mock
    private SliderQueryRepository sliderQueryRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private TracingMetrics tracingMetrics;

    private SliderQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                        .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        service = new SliderQueryServiceImpl(
                sliderQueryRepository,
                redisService,
                tracingMetrics,
                objectMapper);
    }

    private Slider mkSlider(Long id) {
        Slider s = new Slider();
        try {
            Field idField = s.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(s, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        s.setName("Promo");
        s.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        s.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return s;
    }

    @Test
    void findById_Success() {
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(sliderQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(mkSlider(1L)));
        ApiResponse<?> result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void findById_NotFound_ThrowsResourceNotFound() {
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(sliderQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());
        org.junit.jupiter.api.Assertions.assertThrows(ResourceNotFoundException.class,
                () -> service.findById(999).await().indefinitely());
    }

    @Test
    void findAll_Success() {
        FindAllSliderRequest req = new FindAllSliderRequest();
        req.setPage(1);
        req.setPageSize(10);
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        when(sliderQueryRepository.findSliders(any(FindAllSliderRequest.class)))
                .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());
        ApiResponsePagination<List<com.sanedge.slider.domain.response.SliderResponse>> result = service.findAll(req)
                .await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
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