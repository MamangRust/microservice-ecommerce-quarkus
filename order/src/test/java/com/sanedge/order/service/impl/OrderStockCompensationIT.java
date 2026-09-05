package com.sanedge.order.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.common.test.RedisResource;
import com.sanedge.order.domain.requests.CreateOrderItemRequest;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.CreateShippingAddressRequest;
import com.sanedge.order.domain.response.OrderResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.order.repository.OrderCommandRepository;
import com.sanedge.order.repository.OrderQueryRepository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

/**
 * Resilience integration test for order creation with a real PostgreSQL:
 * when the second order item hits insufficient stock, the stock reserved for
 * the first item must be compensated back (adjustStock with the opposite
 * delta) and the partially-created order row must be rolled back so no order
 * remains in the database.
 *
 * <p>The service under test is assembled manually with the real Panache
 * repositories (PostgreSQL via Testcontainers) and Mockito gRPC clients
 * assigned to the package-private fields, because the generated gRPC client
 * beans cannot be replaced via {@code @InjectMock}.</p>
 */
@QuarkusTest
@QuarkusTestResource(value = PostgreSqlResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisResource.class, restrictToAnnotatedClass = true)
@RunOnVertxContext
class OrderStockCompensationIT {

    @Inject
    OrderQueryRepository orderQueryRepo;

    @Inject
    OrderCommandRepository orderCommandRepo;

    @Inject
    Validator validator;

    private RedisService redisService;
    private TracingMetrics tracingMetrics;
    private pb.merchant.MerchantQueryService merchantQueryService;
    private pb.user.UserQueryService userQueryService;
    private pb.product.ProductQueryService productQueryService;
    private pb.product.ProductCommandService productCommandService;
    private pb.order_item.OrderItemCommandService orderItemCommandServiceGrpc;
    private pb.order_item.OrderItemQueryService orderItemQueryServiceGrpc;
    private pb.shipping_address.MutinyShippingCommandServiceGrpc.MutinyShippingCommandServiceStub shippingCommandService;
    private pb.shipping_address.MutinyShippingQueryServiceGrpc.MutinyShippingQueryServiceStub shippingQueryService;
    private pb.transaction.TransactionQueryService transactionQueryService;

    private OrderCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        tracingMetrics = mock(TracingMetrics.class);
        merchantQueryService = mock(pb.merchant.MerchantQueryService.class);
        userQueryService = mock(pb.user.UserQueryService.class);
        productQueryService = mock(pb.product.ProductQueryService.class);
        productCommandService = mock(pb.product.ProductCommandService.class);
        orderItemCommandServiceGrpc = mock(pb.order_item.OrderItemCommandService.class);
        orderItemQueryServiceGrpc = mock(pb.order_item.OrderItemQueryService.class);
        shippingCommandService = mock(pb.shipping_address.MutinyShippingCommandServiceGrpc.MutinyShippingCommandServiceStub.class);
        shippingQueryService = mock(pb.shipping_address.MutinyShippingQueryServiceGrpc.MutinyShippingQueryServiceStub.class);
        transactionQueryService = mock(pb.transaction.TransactionQueryService.class);

        service = new OrderCommandServiceImpl(orderQueryRepo, orderCommandRepo, validator,
                redisService, tracingMetrics);
        service.merchantQueryService = merchantQueryService;
        service.userQueryService = userQueryService;
        service.productQueryService = productQueryService;
        service.productCommandService = productCommandService;
        service.orderItemCommandServiceGrpc = orderItemCommandServiceGrpc;
        service.orderItemQueryServiceGrpc = orderItemQueryServiceGrpc;
        service.shippingCommandService = shippingCommandService;
        service.shippingQueryService = shippingQueryService;
        service.transactionQueryService = transactionQueryService;

        lenient().doAnswer(invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? supplier.get() : null;
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? supplier.get() : null;
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(io.opentelemetry.api.common.Attributes.class), any());

        lenient().when(redisService.deleteReactive(anyString()))
                .thenReturn(Uni.createFrom().voidItem());
        lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                .thenReturn(Uni.createFrom().voidItem());
        lenient().when(redisService.getReactive(anyString()))
                .thenReturn(Uni.createFrom().nullItem());
    }

    private void mockHappyPath() {
        pb.merchant.MerchantCommon.ApiResponseMerchant merchant = pb.merchant.MerchantCommon.ApiResponseMerchant
                .newBuilder()
                .setData(pb.merchant.MerchantCommon.MerchantResponse.newBuilder().setId(100).build())
                .build();
        when(merchantQueryService.findById(any()))
                .thenReturn(Uni.createFrom().item(merchant));

        pb.user.UserCommon.ApiResponseUser user = pb.user.UserCommon.ApiResponseUser.newBuilder()
                .setData(pb.user.UserCommon.UserResponse.newBuilder().setId(100).build())
                .build();
        when(userQueryService.findById(any()))
                .thenReturn(Uni.createFrom().item(user));

        // Product 1 has stock 100 (reserved OK); product 2 has stock 0 (fails).
        when(productQueryService.findById(any()))
                .thenAnswer(invocation -> {
                    pb.product.ProductCommon.FindByIdProductRequest req = invocation.getArgument(0);
                    pb.product.ProductCommon.ProductResponse.Builder b = pb.product.ProductCommon.ProductResponse
                            .newBuilder().setId(req.getId()).setPrice(500);
                    if (req.getId() == 1) {
                        b.setCountInStock(100);
                    } else {
                        b.setCountInStock(0);
                    }
                    return Uni.createFrom().item(pb.product.ProductCommon.ApiResponseProduct.newBuilder()
                            .setData(b.build()).build());
                });

        when(productCommandService.adjustStock(any()))
                .thenReturn(Uni.createFrom().item(pb.product.ProductCommon.ApiResponseProduct
                        .getDefaultInstance()));

        when(orderItemCommandServiceGrpc.createOrderItem(any()))
                .thenReturn(Uni.createFrom().item(pb.order_item.OrderItemCommon.ApiResponseOrderItem
                        .getDefaultInstance()));
        when(shippingCommandService.createShipping(any()))
                .thenReturn(Uni.createFrom().item(pb.shipping_address.ShippingAddressCommon.ApiResponseShipping
                        .getDefaultInstance()));
        when(orderItemCommandServiceGrpc.deleteOrderItemByOrderRollback(any()))
                .thenReturn(Uni.createFrom().item(pb.order_item.OrderItemCommon.ApiResponseOrderItemDelete
                        .getDefaultInstance()));
        when(orderItemQueryServiceGrpc.findOrderItemByOrder(any()))
                .thenReturn(Uni.createFrom().item(pb.order_item.OrderItemCommon.ApiResponsesOrderItem
                        .getDefaultInstance()));
    }

    private CreateOrderRequest requestWithItems(boolean withFailingSecond) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setMerchantId(100);
        request.setUserId(100);

        CreateOrderItemRequest item1 = new CreateOrderItemRequest();
        item1.setProductId(1);
        item1.setQuantity(2);
        item1.setPrice(500);
        if (withFailingSecond) {
            CreateOrderItemRequest item2 = new CreateOrderItemRequest();
            item2.setProductId(2);
            item2.setQuantity(5);
            item2.setPrice(500);
            request.setItems(List.of(item1, item2));
        } else {
            request.setItems(List.of(item1));
        }

        CreateShippingAddressRequest shipping = new CreateShippingAddressRequest();
        shipping.setAlamat("123 Test Street");
        shipping.setProvinsi("Test Province");
        shipping.setKota("Test City");
        shipping.setCourier("Test Courier");
        shipping.setShippingMethod("REG");
        shipping.setShippingCost(1000);
        shipping.setNegara("Indonesia");
        request.setShippingAddress(shipping);

        return request;
    }

    private Uni<Void> cleanOrders() {
        return Panache.withTransaction(() -> orderQueryRepo.deleteAll()).replaceWithVoid();
    }

    @Test
    Uni<Void> createRollsBackAndCompensatesWhenSecondItemFails() {
        mockHappyPath();

        return cleanOrders()
                .chain(() -> service.create(requestWithItems(true))
                        .onFailure().invoke(error -> assertThat(error)
                                .isInstanceOf(com.sanedge.common.exception.InvalidRequestException.class))
                        .onFailure().recoverWithItem(error -> ApiResponse.<OrderResponse>success(
                                "expected failure", null)))
                .chain(() -> Panache.withSession(() -> orderQueryRepo.count()))
                .invoke(count -> assertThat(count).isZero())
                .invoke(() -> {
                    ArgumentCaptor<pb.product.ProductCommand.AdjustProductStockRequest> captor =
                            ArgumentCaptor.forClass(pb.product.ProductCommand.AdjustProductStockRequest.class);
                    verify(productCommandService, times(2)).adjustStock(captor.capture());
                    List<pb.product.ProductCommand.AdjustProductStockRequest> calls = captor.getAllValues();
                    assertThat(calls).hasSize(2);
                    assertThat(calls.get(0).getProductId()).isEqualTo(1);
                    assertThat(calls.get(0).getDelta()).isEqualTo(-2);
                    assertThat(calls.get(1).getProductId()).isEqualTo(1);
                    assertThat(calls.get(1).getDelta()).isEqualTo(2);
                    verify(orderItemCommandServiceGrpc, atLeastOnce())
                            .deleteOrderItemByOrderRollback(any());
                    verify(tracingMetrics, atLeastOnce())
                            .recordStockCompensation(eq("success"), eq(1));
                })
                .replaceWithVoid();
    }

    @Test
    Uni<Void> happyPathDoesNotCompensate() {
        mockHappyPath();

        return cleanOrders()
                .chain(() -> service.create(requestWithItems(false)))
                .invoke(response -> assertThat(response).isNotNull())
                .chain(() -> Panache.withSession(() -> orderQueryRepo.count()))
                .invoke(count -> assertThat(count).isEqualTo(1))
                .invoke(() -> {
                    verify(productCommandService, times(1)).adjustStock(any());
                    verify(tracingMetrics, never()).recordStockCompensation(any(), any());
                })
                .replaceWithVoid();
    }
}
