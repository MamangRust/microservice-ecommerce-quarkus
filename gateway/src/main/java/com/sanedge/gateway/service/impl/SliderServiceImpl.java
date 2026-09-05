package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.SliderDto;
import com.sanedge.gateway.service.SliderService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SliderServiceImpl implements SliderService {

    private static final Logger LOG = Logger.getLogger(SliderServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("slider")
    pb.slider.MutinySliderQueryServiceGrpc.MutinySliderQueryServiceStub sliderQueryService;

    @GrpcClient("slider")
    pb.slider.MutinySliderCommandServiceGrpc.MutinySliderCommandServiceStub sliderCommandService;

    @Override
    public Uni<SliderDto.ApiResponsePaginationSlider> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("slider.findAll", () -> sliderQueryService.findAll(
                pb.slider.SliderQuery.FindAllSliderRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(SliderDto.ApiResponsePaginationSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all sliders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.ApiResponsePaginationSlider> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("slider.findByActive", () -> sliderQueryService.findByActive(
                pb.slider.SliderQuery.FindAllSliderRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(SliderDto.ApiResponsePaginationSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active sliders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.ApiResponsePaginationSlider> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("slider.findByTrashed", () -> sliderQueryService.findByTrashed(
                pb.slider.SliderQuery.FindAllSliderRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(SliderDto.ApiResponsePaginationSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed sliders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.ApiResponseSlider> findById(int id) {
        return telemetryHelper.traceAndMetric("slider.findById", () -> sliderQueryService.findById(
                pb.slider.SliderCommon.FindByIdSliderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(SliderDto.ApiResponseSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find slider " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.ApiResponseSlider> create(pb.slider.SliderCommand.CreateSliderRequest body) {
        return telemetryHelper.traceAndMetric("slider.create", () -> sliderCommandService.create(body)
                .map(SliderDto.ApiResponseSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create slider: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.ApiResponseSlider> update(int id, pb.slider.SliderCommand.UpdateSliderRequest body) {
        pb.slider.SliderCommand.UpdateSliderRequest req = pb.slider.SliderCommand.UpdateSliderRequest.newBuilder(body)
                .setId(id)
                .build();
        return telemetryHelper.traceAndMetric("slider.update", () -> sliderCommandService.update(req)
                .map(SliderDto.ApiResponseSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update slider " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.ApiResponseSlider> delete(int id) {
        return telemetryHelper.traceAndMetric("slider.delete", () -> sliderCommandService.trashedSlider(
                pb.slider.SliderCommon.FindByIdSliderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(SliderDto.ApiResponseSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete slider " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.ApiResponseSlider> restore(int id) {
        return telemetryHelper.traceAndMetric("slider.restore", () -> sliderCommandService.restoreSlider(
                pb.slider.SliderCommon.FindByIdSliderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(SliderDto.ApiResponseSlider::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore slider " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("slider.deletePermanent", () -> sliderCommandService.deleteSliderPermanent(
                pb.slider.SliderCommon.FindByIdSliderRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(SliderDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete slider " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("slider.restoreAll", () -> sliderCommandService.restoreAllSlider(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(SliderDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all sliders: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SliderDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("slider.deleteAllPermanent", () -> sliderCommandService.deleteAllSliderPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(SliderDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all sliders: " + throwable.getMessage(), throwable)));
    }
}
