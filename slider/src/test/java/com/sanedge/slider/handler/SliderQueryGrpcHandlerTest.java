package com.sanedge.slider.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;
import com.sanedge.slider.entity.Slider;
import com.sanedge.slider.repository.SliderQueryRepository;
import com.sanedge.slider.service.SliderQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.slider.SliderCommon;
import pb.slider.SliderQuery;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SliderQueryGrpcHandlerTest {

    @Mock
    private SliderQueryService sliderQueryService;

    @Mock
    private SliderQueryRepository sliderQueryRepository;

    @Mock
    private SliderResponse sliderResponse;

    @Mock
    private SliderResponseDeleteAt sliderResponseDeleteAt;

    @Mock
    private PaginationMeta paginationMeta;

    @Mock
    private ApiResponsePagination<List<SliderResponse>> paginationResponse;

    @Mock
    private ApiResponsePagination<List<SliderResponseDeleteAt>> paginationDeleteAtResponse;

    private SliderQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SliderQueryGrpcHandler();
        handler.sliderQueryService = sliderQueryService;
        handler.sliderQueryRepository = sliderQueryRepository;

        when(sliderResponse.getId()).thenReturn(1L);
        when(sliderResponse.getName()).thenReturn("Main Slider");
        when(sliderResponse.getImage()).thenReturn("image.png");
        when(sliderResponse.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(sliderResponse.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");

        when(sliderResponseDeleteAt.getId()).thenReturn(2L);
        when(sliderResponseDeleteAt.getName()).thenReturn("Trashed Slider");
        when(sliderResponseDeleteAt.getImage()).thenReturn("image2.png");
        when(sliderResponseDeleteAt.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(sliderResponseDeleteAt.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(sliderResponseDeleteAt.getDeletedAt()).thenReturn("2024-01-02 00:00:00.0");

        when(paginationMeta.currentPage()).thenReturn(1);
        when(paginationMeta.pageSize()).thenReturn(10);
        when(paginationMeta.totalPages()).thenReturn(1);
        when(paginationMeta.totalRecords()).thenReturn(1);

        when(paginationResponse.status()).thenReturn("success");
        when(paginationResponse.message()).thenReturn("Sliders retrieved successfully");
        when(paginationResponse.data()).thenReturn(List.of(sliderResponse));
        when(paginationResponse.pagination()).thenReturn(paginationMeta);

        when(paginationDeleteAtResponse.status()).thenReturn("success");
        when(paginationDeleteAtResponse.message()).thenReturn("Trashed sliders retrieved successfully");
        when(paginationDeleteAtResponse.data()).thenReturn(List.of(sliderResponseDeleteAt));
        when(paginationDeleteAtResponse.pagination()).thenReturn(paginationMeta);
    }

    @Test
    @DisplayName("findAll - should return ApiResponsePaginationSlider on success")
    void findAll_Success() {
        SliderQuery.FindAllSliderRequest request = SliderQuery.FindAllSliderRequest.newBuilder()
                .setPage(1).setPageSize(10).setSearch("").build();

        when(sliderQueryService.findAll(any())).thenReturn(Uni.createFrom().item(paginationResponse));

        SliderCommon.ApiResponsePaginationSlider response = handler.findAll(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getPagination().getCurrentPage()).isEqualTo(1);
    }

    @Test
    @DisplayName("findAll - should return INTERNAL on failure")
    void findAll_InternalError() {
        SliderQuery.FindAllSliderRequest request = SliderQuery.FindAllSliderRequest.newBuilder()
                .setPage(1).setPageSize(10).build();

        when(sliderQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.findAll(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("findById - should return ApiResponseSlider on success")
    void findById_Success() throws Exception {
        SliderCommon.FindByIdSliderRequest request = SliderCommon.FindByIdSliderRequest.newBuilder()
                .setId(1).build();

        Slider entity = new Slider();
        Field idField = entity.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, 1L);

        entity.setName("Test Slider");
        entity.setImage("image.png");

        Timestamp now = new Timestamp(System.currentTimeMillis());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        when(sliderQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(entity));

        SliderCommon.ApiResponseSlider response = handler.findById(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getId()).isEqualTo(1);
        assertThat(response.getData().getName()).isEqualTo("Test Slider");
    }

    @Test
    @DisplayName("findById - should return NOT_FOUND when slider not found")
    void findById_NotFound() {
        SliderCommon.FindByIdSliderRequest request = SliderCommon.FindByIdSliderRequest.newBuilder()
                .setId(999).build();

        when(sliderQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        StatusRuntimeException ex = null;
        try {
            handler.findById(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("findById - should return INTERNAL on generic exception")
    void findById_InternalError() {
        SliderCommon.FindByIdSliderRequest request = SliderCommon.FindByIdSliderRequest.newBuilder()
                .setId(1).build();

        when(sliderQueryRepository.findById(anyLong()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.findById(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
    }

    @Test
    @DisplayName("findByActive - should return ApiResponsePaginationSliderDeleteAt on success")
    void findByActive_Success() {
        SliderQuery.FindAllSliderRequest request = SliderQuery.FindAllSliderRequest.newBuilder()
                .setPage(1).setPageSize(10).build();

        when(sliderQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(paginationDeleteAtResponse));

        SliderCommon.ApiResponsePaginationSliderDeleteAt response = handler.findByActive(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getDeletedAt().getValue()).isEqualTo("2024-01-02 00:00:00.0");
    }

    @Test
    @DisplayName("findByTrashed - should return ApiResponsePaginationSliderDeleteAt on success")
    void findByTrashed_Success() {
        SliderQuery.FindAllSliderRequest request = SliderQuery.FindAllSliderRequest.newBuilder()
                .setPage(1).setPageSize(10).build();

        when(sliderQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(paginationDeleteAtResponse));

        SliderCommon.ApiResponsePaginationSliderDeleteAt response = handler.findByTrashed(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
    }
}
