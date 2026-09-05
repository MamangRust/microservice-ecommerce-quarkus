package com.sanedge.slider.handler;

import com.sanedge.slider.domain.requests.CreateSliderRequest;
import com.sanedge.slider.domain.requests.UpdateSliderRequest;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;
import com.sanedge.slider.service.SliderCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.slider.MutinySliderCommandServiceGrpc;
import pb.slider.SliderCommon.ApiResponseSlider;
import pb.slider.SliderCommon.ApiResponseSliderAll;
import pb.slider.SliderCommon.ApiResponseSliderDelete;
import pb.slider.SliderCommon.ApiResponseSliderDeleteAt;
import pb.slider.SliderCommon.FindByIdSliderRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class SliderCommandGrpcHandler extends MutinySliderCommandServiceGrpc.SliderCommandServiceImplBase {

    @Inject
    SliderCommandService sliderCommandService;

    @Override
    public Uni<ApiResponseSlider> create(pb.slider.SliderCommand.CreateSliderRequest request) {
        CreateSliderRequest domainReq = new CreateSliderRequest();
        domainReq.setNama(request.getName());
        domainReq.setFilePath(request.getImage());

        return sliderCommandService.createSlider(domainReq)
                .map(apiResp -> {
                    ApiResponseSlider.Builder builder = ApiResponseSlider.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseSlider> update(pb.slider.SliderCommand.UpdateSliderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        UpdateSliderRequest domainReq = new UpdateSliderRequest();
        domainReq.setId(request.getId());
        domainReq.setNama(request.getName());
        domainReq.setFilePath(request.getImage());

        return sliderCommandService.updateSlider(domainReq)
                .map(apiResp -> {
                    ApiResponseSlider.Builder builder = ApiResponseSlider.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseSliderDeleteAt> trashedSlider(FindByIdSliderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return sliderCommandService.trashedSlider(request.getId())
                .map(apiResp -> {
                    ApiResponseSliderDeleteAt.Builder builder = ApiResponseSliderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseSliderDeleteAt> restoreSlider(FindByIdSliderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return sliderCommandService.restoreSlider(request.getId())
                .map(apiResp -> {
                    ApiResponseSliderDeleteAt.Builder builder = ApiResponseSliderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseSliderDelete> deleteSliderPermanent(FindByIdSliderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return sliderCommandService.deleteSliderPermanent(request.getId())
                .map(apiResp -> ApiResponseSliderDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseSliderAll> restoreAllSlider(com.google.protobuf.Empty request) {
        return sliderCommandService.restoreAllSliders()
                .map(apiResp -> ApiResponseSliderAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseSliderAll> deleteAllSliderPermanent(com.google.protobuf.Empty request) {
        return sliderCommandService.deleteAllSlidersPermanent()
                .map(apiResp -> ApiResponseSliderAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
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
        pb.slider.SliderCommon.SliderResponseDeleteAt.Builder builder = pb.slider.SliderCommon.SliderResponseDeleteAt.newBuilder();
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
}
