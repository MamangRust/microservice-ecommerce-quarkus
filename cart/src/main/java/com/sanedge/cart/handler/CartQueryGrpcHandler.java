package com.sanedge.cart.handler;

import com.sanedge.cart.domain.requests.FindAllCartsRequest;
import com.sanedge.cart.domain.response.CartResponse;
import com.sanedge.cart.service.CartService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.cart.MutinyCartQueryServiceGrpc;
import pb.cart.CartCommon.ApiResponsePaginationCart;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class CartQueryGrpcHandler extends MutinyCartQueryServiceGrpc.CartQueryServiceImplBase {

    @Inject
    CartService cartService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationCart> findAll(pb.cart.CartQuery.FindAllCartRequest request) {
        if (request.getUserId() <= 0) {
            return IdValidator.invalid("User id");
        }
        FindAllCartsRequest domainReq = new FindAllCartsRequest();
        domainReq.setUserId(request.getUserId());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return cartService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationCart.Builder builder = ApiResponsePaginationCart.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (CartResponse r : apiResp.data()) {
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

    private pb.cart.CartCommon.CartResponse toProto(CartResponse r) {
        if (r == null) {
            return pb.cart.CartCommon.CartResponse.getDefaultInstance();
        }
        pb.cart.CartCommon.CartResponse.Builder builder = pb.cart.CartCommon.CartResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId());
        }
        if (r.getProductId() != null) {
            builder.setProductId(r.getProductId());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getPrice() != null) {
            builder.setPrice(r.getPrice());
        }
        if (r.getImage() != null) {
            builder.setImage(r.getImage());
        }
        if (r.getQuantity() != null) {
            builder.setQuantity(r.getQuantity());
        }
        if (r.getWeight() != null) {
            builder.setWeight(r.getWeight());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
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
