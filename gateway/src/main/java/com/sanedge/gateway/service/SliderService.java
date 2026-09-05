package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.SliderDto;
import io.smallrye.mutiny.Uni;

public interface SliderService {
    Uni<SliderDto.ApiResponsePaginationSlider> findAll(int page, int size, String search);
    Uni<SliderDto.ApiResponsePaginationSlider> findByActive(int page, int size, String search);
    Uni<SliderDto.ApiResponsePaginationSlider> findByTrashed(int page, int size, String search);
    Uni<SliderDto.ApiResponseSlider> findById(int id);
    Uni<SliderDto.ApiResponseSlider> create(pb.slider.SliderCommand.CreateSliderRequest body);
    Uni<SliderDto.ApiResponseSlider> update(int id, pb.slider.SliderCommand.UpdateSliderRequest body);
    Uni<SliderDto.ApiResponseSlider> delete(int id);
    Uni<SliderDto.ApiResponseSlider> restore(int id);
    Uni<SliderDto.SimpleResponse> deletePermanent(int id);
    Uni<SliderDto.SimpleResponse> restoreAll();
    Uni<SliderDto.SimpleResponse> deleteAllPermanent();
}
