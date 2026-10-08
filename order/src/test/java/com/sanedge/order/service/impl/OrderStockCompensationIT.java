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

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sanedge.common.adapter.merchant.MerchantPort;
import com.sanedge.common.adapter.model.Merchant;
import com.sanedge.common.adapter.model.OrderItem;
import com.sanedge.common.adapter.model.Product;
import com.sanedge.common.adapter.model.ShippingAddress;
import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.order_item.OrderItemPort;
import com.sanedge.common.adapter.product.ProductPort;
import com.sanedge.common.adapter.shipping_address.ShippingAddressPort;
import com.sanedge.common.adapter.transaction.TransactionPort;
import com.sanedge.common.adapter.user.UserPort;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.common.test.RedisResource;
import com.sanedge.order.domain.requests.CreateOrderItemRequest;
import com.sanedge.order.domain.requests.CreateOrderRequest;
import com.sanedge.order.domain.requests.CreateShippingAddressRequest;
import com.sanedge.order.domain.response.OrderResponse;
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
 * repositories (PostgreSQL via Testcontainers) and Mockito port beans
 * assigned to the package-private fields, because the shared gRPC adapters
 * cannot be replaced via {@code @InjectMock}.</p>
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
    private MerchantPort merchantPort;
    private UserPort userPort;
    private ProductPort productPort;
    private OrderItemPort orderItemPort;
    private TransactionPort transactionPort;
    private ShippingAddressPort shippingAddressPort;

    private OrderCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        tracingMetrics = mock(TracingMetrics.class);
        merchantPort = mock(MerchantPort.class);
        userPort = mock(UserPort.class);
        productPort = mock(ProductPort.class);
        orderItemPort = mock(OrderItemPort.class);
        transactionPort = mock(TransactionPort.class);
        shippingAddressPort = mock(ShippingAddressPort.class);

        service = new OrderCommandServiceImpl(orderQueryRepo, orderCommandRepo, validator,
                redisService, tracingMetrics);
        service.merchantPort = merchantPort;
        service.userPort = userPort;
        service.productPort = productPort;
        service.orderItemPort = orderItemPort;
        service.transactionPort = transactionPort;
        service.shippingAddressPort = shippingAddressPort;

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
        when(merchantPort.findById(anyInt()))
                .thenReturn(Uni.createFrom().item(
                        new Merchant(100, 1, "Merchant", "desc", "addr", "mail@example.com", "0800", "active", null,
                                null)));

        when(userPort.findById(anyInt()))
                .thenReturn(Uni.createFrom().item(
                        new User(100, "John", "Doe", "john@example.com", null, null, null)));

        // Product 1 has stock 100 (reserved OK); product 2 has stock 0 (fails).
        when(productPort.findById(anyInt()))
                .thenAnswer(invocation -> {
                    int id = invocation.getArgument(0);
                    return Uni.createFrom().item(new Product(id, 100, 1, "Product " + id, "desc", 500,
                            id == 1 ? 100 : 0, "brand", 0, 0f, "slug", "image", null, null));
                });

        when(productPort.adjustStock(anyInt(), anyInt()))
                .thenReturn(Uni.createFrom().item(new Product(1, 100, 1, "Product", "desc", 500, 100, "brand", 0, 0f,
                        "slug", "image", null, null)));

        when(orderItemPort.create(any()))
                .thenReturn(Uni.createFrom().item(new OrderItem(1, 1, 1, 2, 500, null, null)));
        when(shippingAddressPort.create(any()))
                .thenReturn(Uni.createFrom().item(
                        new ShippingAddress(1, 1, "addr", "prov", "negara", "kota", "REG", 1000, null, null)));
        when(orderItemPort.deleteByOrderIdRollback(anyInt()))
                .thenReturn(Uni.createFrom().item(true));
        when(orderItemPort.findOrderItemByOrder(anyInt()))
                .thenReturn(Uni.createFrom().item(List.of()));
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
                    ArgumentCaptor<Integer> productIdCaptor = ArgumentCaptor.forClass(Integer.class);
                    ArgumentCaptor<Integer> deltaCaptor = ArgumentCaptor.forClass(Integer.class);
                    verify(productPort, times(2)).adjustStock(productIdCaptor.capture(), deltaCaptor.capture());
                    List<Integer> productIds = productIdCaptor.getAllValues();
                    List<Integer> deltas = deltaCaptor.getAllValues();
                    assertThat(productIds).containsExactly(1, 1);
                    assertThat(deltas).containsExactly(-2, 2);
                    verify(orderItemPort, atLeastOnce()).deleteByOrderIdRollback(anyInt());
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
                    verify(productPort, times(1)).adjustStock(anyInt(), anyInt());
                    verify(tracingMetrics, never()).recordStockCompensation(any(), any());
                })
                .replaceWithVoid();
    }
}
