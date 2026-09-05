package com.sanedge.slider.service;

import com.sanedge.slider.domain.requests.CreateSliderRequest;
import com.sanedge.slider.domain.requests.UpdateSliderRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface SliderCommandService {
    Uni<ApiResponse<SliderResponse>> createSlider(CreateSliderRequest req);

    Uni<ApiResponse<SliderResponse>> updateSlider(UpdateSliderRequest req);

    Uni<ApiResponse<SliderResponseDeleteAt>> trashedSlider(Integer sliderId);

    Uni<ApiResponse<SliderResponseDeleteAt>> restoreSlider(Integer sliderId);

    Uni<ApiResponse<Void>> deleteSliderPermanent(Integer sliderId);

    Uni<ApiResponse<Void>> restoreAllSliders();

    Uni<ApiResponse<Void>> deleteAllSlidersPermanent();
}
