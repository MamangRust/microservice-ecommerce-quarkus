package com.sanedge.shipping_address.handler;

import com.sanedge.shipping_address.domain.response.ShippingAddressResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponseDeleteAt;
import com.sanedge.shipping_address.service.ShippingAddressCommand;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.shipping_address.MutinyShippingCommandServiceGrpc;
import pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest;
import pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest;
import pb.shipping_address.ShippingAddressCommon.ApiResponseShipping;
import pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt;
import pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDelete;
import pb.shipping_address.ShippingAddressCommon.ApiResponseShippingAll;
import pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class ShippingAddressCommandGrpcHandler extends MutinyShippingCommandServiceGrpc.ShippingCommandServiceImplBase {

    @Inject
    ShippingAddressCommand shippingAddressCommand;

    @Override
    public Uni<ApiResponseShipping> createShipping(CreateShippingAddressRequest request) {
        return shippingAddressCommand.create(request)
                .map(apiResp -> {
                    ApiResponseShipping.Builder builder = ApiResponseShipping.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseShipping> updateShipping(UpdateShippingAddressRequest request) {
        if (request.getShippingId() <= 0) {
            return IdValidator.invalid("Shipping id");
        }
        return shippingAddressCommand.update(request)
                .map(apiResp -> {
                    ApiResponseShipping.Builder builder = ApiResponseShipping.newBuilder()
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
    public Uni<ApiResponseShippingDeleteAt> trashedShipping(FindByIdShippingRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return shippingAddressCommand.trash(request.getId())
                .map(apiResp -> {
                    ApiResponseShippingDeleteAt.Builder builder = ApiResponseShippingDeleteAt.newBuilder()
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
    public Uni<ApiResponseShippingDeleteAt> restoreShipping(FindByIdShippingRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return shippingAddressCommand.restore(request.getId())
                .map(apiResp -> {
                    ApiResponseShippingDeleteAt.Builder builder = ApiResponseShippingDeleteAt.newBuilder()
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
    public Uni<ApiResponseShippingDelete> deleteShippingPermanent(FindByIdShippingRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return shippingAddressCommand.deletePermanently(request.getId())
                .map(apiResp -> ApiResponseShippingDelete.newBuilder()
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
    public Uni<ApiResponseShippingAll> restoreAllShipping(com.google.protobuf.Empty request) {
        return shippingAddressCommand.restoreAll()
                .map(apiResp -> ApiResponseShippingAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseShippingDelete> deleteShippingByOrderPermanent(FindByIdShippingRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        // Since there is no delete shipping by order permanent in the service
        // interface, we map it to standard permanent delete
        return shippingAddressCommand.deletePermanently(request.getId())
                .map(apiResp -> ApiResponseShippingDelete.newBuilder()
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
    public Uni<ApiResponseShippingAll> deleteAllShippingPermanent(com.google.protobuf.Empty request) {
        return shippingAddressCommand.deleteAllPermanent()
                .map(apiResp -> ApiResponseShippingAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.shipping_address.ShippingAddressCommon.ShippingResponse toProto(ShippingAddressResponse r) {
        if (r == null) {
            return pb.shipping_address.ShippingAddressCommon.ShippingResponse.getDefaultInstance();
        }
        return pb.shipping_address.ShippingAddressCommon.ShippingResponse.newBuilder()
                .setId(r.getId().intValue())
                .setOrderId(r.getOrderId())
                .setAlamat(r.getAlamat() != null ? r.getAlamat() : "")
                .setProvinsi(r.getProvinsi() != null ? r.getProvinsi() : "")
                .setNegara(r.getNegara() != null ? r.getNegara() : "")
                .setKota(r.getKota() != null ? r.getKota() : "")
                .setShippingMethod(r.getShippingMethod() != null ? r.getShippingMethod() : "")
                .setShippingCost(r.getShippingCost())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    private pb.shipping_address.ShippingAddressCommon.ShippingResponseDeleteAt toProto(
            ShippingAddressResponseDeleteAt r) {
        if (r == null) {
            return pb.shipping_address.ShippingAddressCommon.ShippingResponseDeleteAt.getDefaultInstance();
        }
        pb.shipping_address.ShippingAddressCommon.ShippingResponseDeleteAt.Builder builder = pb.shipping_address.ShippingAddressCommon.ShippingResponseDeleteAt
                .newBuilder()
                .setId(r.getId().intValue())
                .setOrderId(r.getOrderId())
                .setAlamat(r.getAlamat() != null ? r.getAlamat() : "")
                .setProvinsi(r.getProvinsi() != null ? r.getProvinsi() : "")
                .setNegara(r.getNegara() != null ? r.getNegara() : "")
                .setKota(r.getKota() != null ? r.getKota() : "")
                .setShippingMethod(r.getShippingMethod() != null ? r.getShippingMethod() : "")
                .setShippingCost(r.getShippingCost())
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }
}
