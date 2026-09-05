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
class ShippingAddressServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.shipping_address.MutinyShippingQueryServiceGrpc.MutinyShippingQueryServiceStub shippingQueryService;
    @Mock
    private pb.shipping_address.MutinyShippingCommandServiceGrpc.MutinyShippingCommandServiceStub shippingCommandService;

    private ShippingAddressServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = ShippingAddressServiceImpl.class.getDeclaredField(name);
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
        service = new ShippingAddressServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("shippingQueryService", shippingQueryService);
        inject("shippingCommandService", shippingCommandService);
    }

    @Test
    void findById_PropagatesAddressResponse() {
        pb.shipping_address.ShippingAddressCommon.ApiResponseShipping proto = pb.shipping_address.ShippingAddressCommon.ApiResponseShipping.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(shippingQueryService.findById(any(pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesAddressResponse() {
        pb.shipping_address.ShippingAddressCommon.ApiResponseShipping proto = pb.shipping_address.ShippingAddressCommon.ApiResponseShipping.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest req = pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest.newBuilder().build();
        lenient().when(shippingCommandService.createShipping(any(pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt proto = pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(shippingCommandService.trashedShipping(any(pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt proto = pb.shipping_address.ShippingAddressCommon.ApiResponseShippingDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(shippingCommandService.restoreShipping(any(pb.shipping_address.ShippingAddressCommon.FindByIdShippingRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
