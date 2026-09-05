package com.sanedge.slider.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;
import com.sanedge.slider.service.SliderCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.slider.SliderCommand;
import pb.slider.SliderCommon;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SliderCommandGrpcHandlerTest {

        @Mock
        private SliderCommandService sliderCommandService;

        @Mock
        private SliderResponse sliderResponse;

        @Mock
        private SliderResponseDeleteAt sliderResponseDeleteAt;

        @Mock
        private ApiResponse<SliderResponse> apiResponseSuccess;

        @Mock
        private ApiResponse<SliderResponseDeleteAt> apiResponseDeleteAtSuccess;

        @Mock
        private ApiResponse<Void> apiResponseEmptySuccess;

        private SliderCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new SliderCommandGrpcHandler();
                handler.sliderCommandService = sliderCommandService;

                when(sliderResponse.getId()).thenReturn(1L);
                when(sliderResponse.getName()).thenReturn("Created Slider");
                when(sliderResponse.getImage()).thenReturn("image.png");
                when(sliderResponse.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
                when(sliderResponse.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");

                when(sliderResponseDeleteAt.getId()).thenReturn(1L);
                when(sliderResponseDeleteAt.getName()).thenReturn("Trashed Slider");
                when(sliderResponseDeleteAt.getImage()).thenReturn("image.png");
                when(sliderResponseDeleteAt.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
                when(sliderResponseDeleteAt.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");
                when(sliderResponseDeleteAt.getDeletedAt()).thenReturn("2024-01-02 00:00:00.0");

                when(apiResponseSuccess.status()).thenReturn("success");
                when(apiResponseSuccess.message()).thenReturn("Operation successful");
                when(apiResponseSuccess.data()).thenReturn(sliderResponse);

                when(apiResponseDeleteAtSuccess.status()).thenReturn("success");
                when(apiResponseDeleteAtSuccess.message()).thenReturn("Operation successful");
                when(apiResponseDeleteAtSuccess.data()).thenReturn(sliderResponseDeleteAt);

                when(apiResponseEmptySuccess.status()).thenReturn("success");
                when(apiResponseEmptySuccess.message()).thenReturn("Bulk operation successful");
                when(apiResponseEmptySuccess.data()).thenReturn(null);
        }

        @Test
        @DisplayName("create - should return ApiResponseSlider on success")
        void create_Success() {
                SliderCommand.CreateSliderRequest request = SliderCommand.CreateSliderRequest.newBuilder()
                                .setName("New Slider").setImage("image.png").build();

                when(sliderCommandService.createSlider(any())).thenReturn(Uni.createFrom().item(apiResponseSuccess));

                SliderCommon.ApiResponseSlider response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getName()).isEqualTo("Created Slider");
        }

        @Test
        @DisplayName("create - should return INTERNAL on failure")
        void create_InternalError() {
                SliderCommand.CreateSliderRequest request = SliderCommand.CreateSliderRequest.newBuilder().build();

                when(sliderCommandService.createSlider(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.create(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("update - should return ApiResponseSlider on success")
        void update_Success() {
                SliderCommand.UpdateSliderRequest request = SliderCommand.UpdateSliderRequest.newBuilder()
                                .setId(1).setName("Updated Slider").setImage("image2.png").build();

                when(sliderCommandService.updateSlider(any())).thenReturn(Uni.createFrom().item(apiResponseSuccess));

                SliderCommon.ApiResponseSlider response = handler.update(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("trashedSlider - should return ApiResponseSliderDeleteAt on success")
        void trashedSlider_Success() {
                SliderCommon.FindByIdSliderRequest request = SliderCommon.FindByIdSliderRequest.newBuilder().setId(1)
                                .build();

                when(sliderCommandService.trashedSlider(any()))
                                .thenReturn(Uni.createFrom().item(apiResponseDeleteAtSuccess));

                SliderCommon.ApiResponseSliderDeleteAt response = handler.trashedSlider(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getDeletedAt().getValue()).isEqualTo("2024-01-02 00:00:00.0");
        }

        @Test
        @DisplayName("restoreSlider - should return ApiResponseSliderDeleteAt on success")
        void restoreSlider_Success() {
                SliderCommon.FindByIdSliderRequest request = SliderCommon.FindByIdSliderRequest.newBuilder().setId(1)
                                .build();

                when(sliderCommandService.restoreSlider(any()))
                                .thenReturn(Uni.createFrom().item(apiResponseDeleteAtSuccess));

                SliderCommon.ApiResponseSliderDeleteAt response = handler.restoreSlider(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("deleteSliderPermanent - should return ApiResponseSliderDelete on success")
        void deleteSliderPermanent_Success() {
                SliderCommon.FindByIdSliderRequest request = SliderCommon.FindByIdSliderRequest.newBuilder().setId(1)
                                .build();

                when(sliderCommandService.deleteSliderPermanent(any()))
                                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

                SliderCommon.ApiResponseSliderDelete response = handler.deleteSliderPermanent(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Bulk operation successful");
        }

        @Test
        @DisplayName("restoreAllSlider - should return ApiResponseSliderAll on success")
        void restoreAllSlider_Success() {
                when(sliderCommandService.restoreAllSliders())
                                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

                SliderCommon.ApiResponseSliderAll response = handler
                                .restoreAllSlider(com.google.protobuf.Empty.getDefaultInstance()).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("deleteAllSliderPermanent - should return ApiResponseSliderAll on success")
        void deleteAllSliderPermanent_Success() {
                when(sliderCommandService.deleteAllSlidersPermanent())
                                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

                SliderCommon.ApiResponseSliderAll response = handler
                                .deleteAllSliderPermanent(com.google.protobuf.Empty.getDefaultInstance()).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }
}
