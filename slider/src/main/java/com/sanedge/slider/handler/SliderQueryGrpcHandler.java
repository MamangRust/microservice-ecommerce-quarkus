package com.sanedge.slider.handler;

import com.sanedge.slider.domain.requests.FindAllSliderRequest;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;
import com.sanedge.slider.repository.SliderQueryRepository;
import com.sanedge.slider.service.SliderQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.slider.MutinySliderQueryServiceGrpc;
import pb.slider.SliderCommon.ApiResponsePaginationSlider;
import pb.slider.SliderCommon.ApiResponsePaginationSliderDeleteAt;
import pb.slider.SliderCommon.ApiResponseSlider;
import pb.slider.SliderCommon.FindByIdSliderRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class SliderQueryGrpcHandler extends MutinySliderQueryServiceGrpc.SliderQueryServiceImplBase {

    @Inject
    SliderQueryService sliderQueryService;

    @Inject
    SliderQueryRepository sliderQueryRepository;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationSlider> findAll(pb.slider.SliderQuery.FindAllSliderRequest request) {
        FindAllSliderRequest domainReq = new FindAllSliderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return sliderQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationSlider.Builder builder = ApiResponsePaginationSlider.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (SliderResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponseSlider> findById(FindByIdSliderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return sliderQueryRepository.findById((long) request.getId())
                .map(entity -> {
                    if (entity == null) {
                        throw new com.sanedge.common.exception.ResourceNotFoundException("Slider not found");
                    }
                    return ApiResponseSlider.newBuilder()
                            .setStatus("success")
                            .setMessage("Slider retrieved successfully")
                            .setData(toProto(SliderResponse.from(entity)))
                            .build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationSliderDeleteAt> findByActive(pb.slider.SliderQuery.FindAllSliderRequest request) {
        FindAllSliderRequest domainReq = new FindAllSliderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return sliderQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationSliderDeleteAt.Builder builder = ApiResponsePaginationSliderDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (SliderResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationSliderDeleteAt> findByTrashed(pb.slider.SliderQuery.FindAllSliderRequest request) {
        FindAllSliderRequest domainReq = new FindAllSliderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return sliderQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationSliderDeleteAt.Builder builder = ApiResponsePaginationSliderDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (SliderResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.slider.SliderCommon.SliderResponse toProto(SliderResponse r) {
        if (r == null) {
            return pb.slider.SliderCommon.SliderResponse.getDefaultInstance();
        }
        pb.slider.SliderCommon.SliderResponse.Builder builder = pb.slider.SliderCommon.SliderResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getImage() != null) {
            builder.setImage(r.getImage());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.slider.SliderCommon.SliderResponseDeleteAt toProto(SliderResponseDeleteAt r) {
        if (r == null) {
            return pb.slider.SliderCommon.SliderResponseDeleteAt.getDefaultInstance();
        }
        pb.slider.SliderCommon.SliderResponseDeleteAt.Builder builder = pb.slider.SliderCommon.SliderResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getImage() != null) {
            builder.setImage(r.getImage());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }

    private pb.Api.PaginationMeta toProto(com.sanedge.common.domain.response.PaginationMeta m) {
        if (m == null) {
            return pb.Api.PaginationMeta.getDefaultInstance();
        }
        return pb.Api.PaginationMeta.newBuilder()
                .setCurrentPage(m.currentPage())
                .setPageSize(m.pageSize())
                .setTotalPages(m.totalPages())
                .setTotalRecords(m.totalRecords())
                .build();
    }
}
