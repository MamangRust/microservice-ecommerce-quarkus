package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.CartDto;
import com.sanedge.gateway.service.CartService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CartServiceImpl implements CartService {

    private static final Logger LOG = Logger.getLogger(CartServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("cart")
    pb.cart.MutinyCartQueryServiceGrpc.MutinyCartQueryServiceStub cartQueryService;

    @GrpcClient("cart")
    pb.cart.MutinyCartCommandServiceGrpc.MutinyCartCommandServiceStub cartCommandService;

    @Override
    public Uni<CartDto.ApiResponsePaginationCart> findAll(int userId, int page, int size, String search) {
        return telemetryHelper.traceAndMetric("cart.findAll", () -> cartQueryService.findAll(
                pb.cart.CartQuery.FindAllCartRequest.newBuilder()
                        .setUserId(userId)
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(CartDto.ApiResponsePaginationCart::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all cart items: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CartDto.ApiResponseCart> create(pb.cart.CartCommand.CreateCartRequest body) {
        return telemetryHelper.traceAndMetric("cart.create", () -> cartCommandService.create(body)
                .map(CartDto.ApiResponseCart::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to add item to cart: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CartDto.SimpleResponse> delete(int cartId, int userId) {
        return telemetryHelper.traceAndMetric("cart.delete", () -> cartCommandService.delete(
                pb.cart.CartCommand.DeleteCartRequest.newBuilder()
                        .setCartId(cartId)
                        .setUserId(userId)
                        .build())
                .map(CartDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to delete cart item " + cartId + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CartDto.SimpleResponse> deleteAll(int userId, java.util.List<Integer> cartIds) {
        pb.cart.CartCommand.DeleteAllCartRequest.Builder builder = pb.cart.CartCommand.DeleteAllCartRequest.newBuilder()
                .setUserId(userId);
        if (cartIds != null) {
            builder.addAllCartIds(cartIds);
        }
        return telemetryHelper.traceAndMetric("cart.deleteAll", () -> cartCommandService.deleteAll(builder.build())
                .map(CartDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to clear cart for user " + userId + ": " + throwable.getMessage(), throwable)));
    }
}
