package com.sanedge.cart.handler;

import com.sanedge.cart.domain.requests.CreateCartRequest;
import com.sanedge.cart.domain.response.CartResponse;
import com.sanedge.cart.service.CartService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.cart.MutinyCartCommandServiceGrpc;
import pb.cart.CartCommon.ApiResponseCart;
import pb.cart.CartCommon.ApiResponseCartDelete;
import pb.cart.CartCommon.ApiResponseCartAll;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class CartCommandGrpcHandler extends MutinyCartCommandServiceGrpc.CartCommandServiceImplBase {

    @Inject
    CartService cartService;

    @Override
    public Uni<ApiResponseCart> create(pb.cart.CartCommand.CreateCartRequest request) {
        if (request.getProductId() <= 0) {
            return IdValidator.invalid("Product id");
        }
        if (request.getUserId() <= 0) {
            return IdValidator.invalid("User id");
        }
        CreateCartRequest domainReq = new CreateCartRequest();
        domainReq.setQuantity(request.getQuantity());
        domainReq.setProductId(request.getProductId());
        domainReq.setUserId(request.getUserId());

        return cartService.createCart(domainReq)
                .map(apiResp -> {
                    ApiResponseCart.Builder builder = ApiResponseCart.newBuilder()
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
    public Uni<ApiResponseCartDelete> delete(pb.cart.CartCommand.DeleteCartRequest request) {
        if (request.getCartId() <= 0) {
            return IdValidator.invalid("Cart id");
        }
        return cartService.deletePermanent((long) request.getCartId())
                .map(apiResp -> ApiResponseCartDelete.newBuilder()
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
    public Uni<ApiResponseCartAll> deleteAll(pb.cart.CartCommand.DeleteAllCartRequest request) {
        com.sanedge.cart.domain.requests.DeleteCartRequest domainReq = new com.sanedge.cart.domain.requests.DeleteCartRequest();
        domainReq.setCartIds(request.getCartIdsList());

        return cartService.deleteAllPermanently(domainReq)
                .map(apiResp -> ApiResponseCartAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
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
}
