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
class MerchantServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;
    @Mock
    private pb.merchant.MutinyMerchantCommandServiceGrpc.MutinyMerchantCommandServiceStub merchantCommandService;

    private MerchantServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = MerchantServiceImpl.class.getDeclaredField(name);
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
        service = new MerchantServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("merchantQueryService", merchantQueryService);
        inject("merchantCommandService", merchantCommandService);
    }

    @Test
    void getMerchant_PropagatesMerchantResponse() {
        pb.merchant.MerchantCommon.ApiResponseMerchant proto = pb.merchant.MerchantCommon.ApiResponseMerchant.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(merchantQueryService.findById(any(pb.merchant.MerchantCommon.FindByIdMerchantRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.getMerchant(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createMerchant_PropagatesMerchantResponse() {
        pb.merchant.MerchantCommon.ApiResponseMerchant proto = pb.merchant.MerchantCommon.ApiResponseMerchant.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.merchant.MerchantCommand.CreateMerchantRequest req = pb.merchant.MerchantCommand.CreateMerchantRequest.newBuilder().build();
        lenient().when(merchantCommandService.create(any(pb.merchant.MerchantCommand.CreateMerchantRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.createMerchant(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void deleteMerchant_TrashStub_PropagatesMerchantDeleteAt() {
        pb.merchant.MerchantCommon.ApiResponseMerchantDeleteAt proto = pb.merchant.MerchantCommon.ApiResponseMerchantDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(merchantCommandService.trashedMerchant(any(pb.merchant.MerchantCommon.FindByIdMerchantRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deleteMerchant(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }

    @Test
    void deleteMerchantPermanent_PropagatesSimpleResponse() {
        pb.merchant.MerchantCommon.ApiResponseMerchantDelete proto = pb.merchant.MerchantCommon.ApiResponseMerchantDelete.newBuilder()
                .setStatus("success").setMessage("deleted").build();
        lenient().when(merchantCommandService.deleteMerchantPermanent(any(pb.merchant.MerchantCommon.FindByIdMerchantRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.deleteMerchantPermanent(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.merchant.MerchantCommon.ApiResponseMerchant proto = pb.merchant.MerchantCommon.ApiResponseMerchant.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(merchantCommandService.restoreMerchant(any(pb.merchant.MerchantCommon.FindByIdMerchantRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restoreMerchant(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
