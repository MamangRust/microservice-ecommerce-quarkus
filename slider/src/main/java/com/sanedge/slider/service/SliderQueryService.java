package com.sanedge.slider.service;

import java.util.List;

import com.sanedge.slider.domain.requests.FindAllSliderRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface SliderQueryService {
    Uni<ApiResponsePagination<List<SliderResponse>>> findAll(FindAllSliderRequest req);

    Uni<ApiResponsePagination<List<SliderResponseDeleteAt>>> findByActive(FindAllSliderRequest req);

    Uni<ApiResponsePagination<List<SliderResponseDeleteAt>>> findByTrashed(FindAllSliderRequest req);

    Uni<ApiResponse<SliderResponse>> findById(Integer id);
}
