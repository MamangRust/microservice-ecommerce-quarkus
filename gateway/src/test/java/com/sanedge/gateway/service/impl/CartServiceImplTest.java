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
class CartServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.cart.MutinyCartQueryServiceGrpc.MutinyCartQueryServiceStub cartQueryService;
    @Mock
    private pb.cart.MutinyCartCommandServiceGrpc.MutinyCartCommandServiceStub cartCommandService;

    private CartServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = CartServiceImpl.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(service, value);
    }

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Uni<?>> supplier = invocation.getArgument(1);
                    return supplier.get();
                });
        service = new CartServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("cartQueryService", cartQueryService);
        inject("cartCommandService", cartCommandService);
    }

    @Test
    void create_PropagatesCartResponse() {
        pb.cart.CartCommon.ApiResponseCart proto = pb.cart.CartCommon.ApiResponseCart.newBuilder()
                .setStatus("success").setMessage("added").build();
        pb.cart.CartCommand.CreateCartRequest req = pb.cart.CartCommand.CreateCartRequest.newBuilder().build();
        lenient().when(cartCommandService.create(any(pb.cart.CartCommand.CreateCartRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void delete_PropagatesSimpleResponse() {
        pb.cart.CartCommon.ApiResponseCartDelete proto = pb.cart.CartCommon.ApiResponseCartDelete.newBuilder()
                .setStatus("success").setMessage("deleted").build();
        lenient().when(cartCommandService.delete(any(pb.cart.CartCommand.DeleteCartRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1, 1).await().indefinitely();
        assertThat(result.message()).isEqualTo("deleted");
    }

    @Test
    void findAll_PropagatesPaginatedResponse() {
        pb.cart.CartCommon.ApiResponsePaginationCart proto = pb.cart.CartCommon.ApiResponsePaginationCart.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(cartQueryService.findAll(any(pb.cart.CartQuery.FindAllCartRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findAll(1, 1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
