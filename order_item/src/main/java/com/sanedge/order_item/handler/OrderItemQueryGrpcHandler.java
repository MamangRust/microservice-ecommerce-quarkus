package com.sanedge.order_item.handler;

import java.util.stream.Collectors;

import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.order_item.domain.requests.FindAllOrderItemRequest;
import com.sanedge.order_item.domain.response.OrderItemResponse;
import com.sanedge.order_item.domain.response.OrderItemResponseDeleteAt;
import com.sanedge.order_item.service.OrderItemQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order_item.MutinyOrderItemQueryServiceGrpc;
import pb.order_item.OrderItemCommon.ApiResponsePaginationOrderItem;
import pb.order_item.OrderItemCommon.ApiResponsePaginationOrderItemDeleteAt;
import pb.order_item.OrderItemCommon.ApiResponsesOrderItem;
import pb.order_item.OrderItemCommon.FindByIdOrderItemRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class OrderItemQueryGrpcHandler extends MutinyOrderItemQueryServiceGrpc.OrderItemQueryServiceImplBase {

    @Inject
    OrderItemQueryService orderItemQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationOrderItem> findAll(pb.order_item.OrderItemQuery.FindAllOrderItemRequest request) {
        FindAllOrderItemRequest domainReq = new FindAllOrderItemRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderItemQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrderItem.Builder builder = ApiResponsePaginationOrderItem.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream()
                                .map(this::toProto)
                                .collect(Collectors.toList()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationOrderItemDeleteAt> findByActive(pb.order_item.OrderItemQuery.FindAllOrderItemRequest request) {
        FindAllOrderItemRequest domainReq = new FindAllOrderItemRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderItemQueryService.findActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrderItemDeleteAt.Builder builder = ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream()
                                .map(this::toProto)
                                .collect(Collectors.toList()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationOrderItemDeleteAt> findByTrashed(pb.order_item.OrderItemQuery.FindAllOrderItemRequest request) {
        FindAllOrderItemRequest domainReq = new FindAllOrderItemRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderItemQueryService.findTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrderItemDeleteAt.Builder builder = ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream()
                                .map(this::toProto)
                                .collect(Collectors.toList()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsesOrderItem> findOrderItemByOrder(FindByIdOrderItemRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return orderItemQueryService.findByOrder((long) request.getId())
                .map(apiResp -> {
                    ApiResponsesOrderItem.Builder builder = ApiResponsesOrderItem.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream()
                                .map(this::toProto)
                                .collect(Collectors.toList()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    private pb.order_item.OrderItemCommon.OrderItemResponse toProto(OrderItemResponse r) {
        if (r == null) {
            return pb.order_item.OrderItemCommon.OrderItemResponse.getDefaultInstance();
        }
        pb.order_item.OrderItemCommon.OrderItemResponse.Builder builder = pb.order_item.OrderItemCommon.OrderItemResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getOrderId() != null) {
            builder.setOrderId(r.getOrderId());
        }
        if (r.getProductId() != null) {
            builder.setProductId(r.getProductId());
        }
        if (r.getQuantity() != null) {
            builder.setQuantity(r.getQuantity());
        }
        if (r.getPrice() != null) {
            builder.setPrice(r.getPrice());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.order_item.OrderItemCommon.OrderItemResponseDeleteAt toProto(OrderItemResponseDeleteAt r) {
        if (r == null) {
            return pb.order_item.OrderItemCommon.OrderItemResponseDeleteAt.getDefaultInstance();
        }
        pb.order_item.OrderItemCommon.OrderItemResponseDeleteAt.Builder builder = pb.order_item.OrderItemCommon.OrderItemResponseDeleteAt.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getOrderId() != null) {
            builder.setOrderId(r.getOrderId());
        }
        if (r.getProductId() != null) {
            builder.setProductId(r.getProductId());
        }
        if (r.getQuantity() != null) {
            builder.setQuantity(r.getQuantity());
        }
        if (r.getPrice() != null) {
            builder.setPrice(r.getPrice());
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
