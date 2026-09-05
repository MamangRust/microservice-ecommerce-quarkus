package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.order.MutinyOrderQueryServiceGrpc.MutinyOrderQueryServiceStub orderQueryService;
    @Mock
    private pb.order.MutinyOrderCommandServiceGrpc.MutinyOrderCommandServiceStub orderCommandService;

    private OrderServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = OrderServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    private void injectNull(String name) throws Exception {
        inject(name, null);
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Uni<?>> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        service = new OrderServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("orderQueryService", orderQueryService);
        inject("orderCommandService", orderCommandService);

        injectNull("orderRevenueService");
        injectNull("orderTotalRevenueService");
        injectNull("orderRevenueByMerchantStub");
        injectNull("orderTotalRevenueByMerchantStub");
    }

    @Test
    void findById_PropagatesOrderResponse() {
        pb.order.OrderCommon.ApiResponseOrder proto = pb.order.OrderCommon.ApiResponseOrder.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(orderQueryService.findById(any(pb.order.OrderCommon.FindByIdOrderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesOrderResponse() {
        pb.order.OrderCommon.ApiResponseOrder proto = pb.order.OrderCommon.ApiResponseOrder.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.order.OrderCommand.CreateOrderRequest req = pb.order.OrderCommand.CreateOrderRequest.newBuilder().build();
        lenient().when(orderCommandService.create(any(pb.order.OrderCommand.CreateOrderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_PropagatesOrderDeleteAt() {
        pb.order.OrderCommon.ApiResponseOrderDeleteAt proto = pb.order.OrderCommon.ApiResponseOrderDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(orderCommandService.trashedOrder(any(pb.order.OrderCommon.FindByIdOrderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.order.OrderCommon.ApiResponseOrderDeleteAt proto = pb.order.OrderCommon.ApiResponseOrderDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(orderCommandService.restoreOrder(any(pb.order.OrderCommon.FindByIdOrderRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
