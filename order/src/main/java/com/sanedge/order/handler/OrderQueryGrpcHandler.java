package com.sanedge.order.handler;

import java.util.stream.Collectors;

import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.order.domain.response.OrderResponseDeleteAt;
import com.sanedge.order.service.OrderQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.order.MutinyOrderQueryServiceGrpc;
import pb.order.OrderCommon.ApiResponseOrder;
import pb.order.OrderCommon.ApiResponsePaginationOrder;
import pb.order.OrderCommon.ApiResponsePaginationOrderDeleteAt;
import pb.order.OrderCommon.FindByIdOrderRequest;
import pb.order.OrderQuery;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class OrderQueryGrpcHandler extends MutinyOrderQueryServiceGrpc.OrderQueryServiceImplBase {

    @Inject
    OrderQueryService orderQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationOrder> findAll(OrderQuery.FindAllOrderRequest request) {
        FindAllOrderRequest domainReq = new FindAllOrderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrder.Builder builder = ApiResponsePaginationOrder.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream().map(this::toProto).collect(Collectors.toList()));
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.Api.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponseOrder> findById(FindByIdOrderRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return orderQueryService.findById((long) request.getId())
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
    @WithSession
    public Uni<ApiResponsePaginationOrderDeleteAt> findByActive(OrderQuery.FindAllOrderRequest request) {
        FindAllOrderRequest domainReq = new FindAllOrderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrderDeleteAt.Builder builder = ApiResponsePaginationOrderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream().map(this::toProto).collect(Collectors.toList()));
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.Api.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationOrderDeleteAt> findByTrashed(OrderQuery.FindAllOrderRequest request) {
        FindAllOrderRequest domainReq = new FindAllOrderRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return orderQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationOrderDeleteAt.Builder builder = ApiResponsePaginationOrderDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream().map(this::toProto).collect(Collectors.toList()));
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(pb.Api.PaginationMeta.newBuilder()
                                .setCurrentPage(apiResp.pagination().currentPage())
                                .setPageSize(apiResp.pagination().pageSize())
                                .setTotalPages(apiResp.pagination().totalPages())
                                .setTotalRecords(apiResp.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                })
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
