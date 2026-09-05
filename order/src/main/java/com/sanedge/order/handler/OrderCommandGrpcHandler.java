package com.sanedge.order.handler;

import java.util.stream.Collectors;

import com.sanedge.order.domain.requests.CreateOrderItemRequest;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.CreateShippingAddressRequest;
import com.sanedge.order.domain.requests.UpdateOrderItemRequest;
import com.sanedge.order.domain.requests.UpdateOrderRequest;
import com.sanedge.order.domain.requests.UpdateShippingAddressRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order.MutinyOrderCommandServiceGrpc;
import pb.order.OrderCommand;
import pb.order.OrderCommon.ApiResponseOrder;
import pb.order.OrderCommon.ApiResponseOrderAll;
import pb.order.OrderCommon.ApiResponseOrderDelete;
import pb.order.OrderCommon.ApiResponseOrderDeleteAt;
import pb.order.OrderCommon.FindByIdOrderRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class OrderCommandGrpcHandler extends MutinyOrderCommandServiceGrpc.OrderCommandServiceImplBase {

    @Inject
    OrderCommandService orderCommandService;

    @Override
    public Uni<ApiResponseOrder> create(OrderCommand.CreateOrderRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        if (request.getUserId() <= 0) {
            return IdValidator.invalid("User id");
        }
        for (OrderCommand.CreateOrderItemRequest item : request.getItemsList()) {
            if (item.getProductId() <= 0) {
                return IdValidator.invalid("Product id");
            }
        }
        CreateOrderRequest domainReq = new CreateOrderRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setUserId(request.getUserId());

        domainReq.setItems(request.getItemsList().stream().map(item -> {
            CreateOrderItemRequest orderItem = new CreateOrderItemRequest();
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());

            return orderItem;
        }).collect(Collectors.toList()));

        if (request.hasShipping()) {
            CreateShippingAddressRequest shippingReq = new CreateShippingAddressRequest();
            shippingReq.setOrderId(request.getShipping().getOrderId());
            shippingReq.setAlamat(request.getShipping().getAlamat());
            shippingReq.setProvinsi(request.getShipping().getProvinsi());
            shippingReq.setKota(request.getShipping().getKota());
            shippingReq.setCourier(request.getShipping().getCourier());
            shippingReq.setShippingMethod(request.getShipping().getShippingMethod());
            shippingReq.setShippingCost(request.getShipping().getShippingCost());
            shippingReq.setNegara(request.getShipping().getNegara());
            domainReq.setShippingAddress(shippingReq);
        }

        return orderCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseOrder.Builder builder = ApiResponseOrder.newBuilder()
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
    public Uni<ApiResponseOrder> update(OrderCommand.UpdateOrderRequest request) {
        if (request.getOrderId() <= 0) {
            return IdValidator.invalid("Order id");
        }
        if (request.getUserId() <= 0) {
            return IdValidator.invalid("User id");
        }
        for (OrderCommand.UpdateOrderItemRequest item : request.getItemsList()) {
            if (item.getProductId() <= 0) {
                return IdValidator.invalid("Product id");
            }
        }
        if (request.hasShipping() && request.getShipping().getShippingId() <= 0) {
            return IdValidator.invalid("Shipping id");
        }
        UpdateOrderRequest domainReq = new UpdateOrderRequest();
        domainReq.setOrderId(request.getOrderId());
        domainReq.setUserId(request.getUserId());

        domainReq.setItems(request.getItemsList().stream().map(item -> {
            UpdateOrderItemRequest orderItem = new UpdateOrderItemRequest();
            orderItem.setOrderItemId(item.getOrderItemId());
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());
            return orderItem;
        }).collect(Collectors.toList()));

        if (request.hasShipping()) {
            UpdateShippingAddressRequest shippingReq = new UpdateShippingAddressRequest();
            shippingReq.setShippingId(request.getShipping().getShippingId());
            shippingReq.setOrderId(request.getShipping().getOrderId());
            shippingReq.setAlamat(request.getShipping().getAlamat());
            shippingReq.setProvinsi(request.getShipping().getProvinsi());
            shippingReq.setKota(request.getShipping().getKota());
            shippingReq.setCourier(request.getShipping().getCourier());
            shippingReq.setShippingMethod(request.getShipping().getShippingMethod());
            shippingReq.setShippingCost(request.getShipping().getShippingCost());
            shippingReq.setNegara(request.getShipping().getNegara());
            domainReq.setShippingAddress(shippingReq);
        }

        return orderCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseOrder.Builder builder = ApiResponseOrder.newBuilder()
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
    public Uni<ApiResponseOrderDeleteAt> trashedOrder(FindByIdOrderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return orderCommandService.trash((long) request.getId())
                .map(apiResp -> {
                    ApiResponseOrderDeleteAt.Builder builder = ApiResponseOrderDeleteAt.newBuilder()
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
    public Uni<ApiResponseOrderDeleteAt> restoreOrder(FindByIdOrderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return orderCommandService.restore((long) request.getId())
                .map(apiResp -> {
                    ApiResponseOrderDeleteAt.Builder builder = ApiResponseOrderDeleteAt.newBuilder()
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
    public Uni<ApiResponseOrderDelete> deleteOrderPermanent(FindByIdOrderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return orderCommandService.delete((long) request.getId())
                .map(apiResp -> ApiResponseOrderDelete.newBuilder()
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
    public Uni<ApiResponseOrderAll> restoreAllOrder(com.google.protobuf.Empty request) {
        return orderCommandService.restoreAll()
                .map(apiResp -> ApiResponseOrderAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseOrderAll> deleteAllOrderPermanent(com.google.protobuf.Empty request) {
        return orderCommandService.deleteAll()
                .map(apiResp -> ApiResponseOrderAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.order.OrderCommon.OrderResponse toProto(OrderResponse r) {
        if (r == null) {
            return pb.order.OrderCommon.OrderResponse.getDefaultInstance();
        }
        pb.order.OrderCommon.OrderResponse.Builder builder = pb.order.OrderCommon.OrderResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId());
        }
        if (r.getTotalPrice() != null) {
            builder.setTotalPrice(r.getTotalPrice());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.order.OrderCommon.OrderResponseDeleteAt toProto(OrderResponseDeleteAt r) {
        if (r == null) {
            return pb.order.OrderCommon.OrderResponseDeleteAt.getDefaultInstance();
        }
        pb.order.OrderCommon.OrderResponseDeleteAt.Builder builder = pb.order.OrderCommon.OrderResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId());
        }
        if (r.getTotalPrice() != null) {
            builder.setTotalPrice(r.getTotalPrice());
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
