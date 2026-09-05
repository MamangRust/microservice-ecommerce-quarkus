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
class MerchantAwardServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.merchant_award.MutinyMerchantAwardQueryServiceGrpc.MutinyMerchantAwardQueryServiceStub merchantAwardQueryService;
    @Mock
    private pb.merchant_award.MutinyMerchantAwardCommandServiceGrpc.MutinyMerchantAwardCommandServiceStub merchantAwardCommandService;

    private MerchantAwardServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = MerchantAwardServiceImpl.class.getDeclaredField(name);
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
        service = new MerchantAwardServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("merchantAwardQueryService", merchantAwardQueryService);
        inject("merchantAwardCommandService", merchantAwardCommandService);
    }

    @Test
    void findById_PropagatesAwardResponse() {
        pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward proto = pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(merchantAwardQueryService.findById(any(pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesAwardResponse() {
        pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward proto = pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest req = pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest.newBuilder().build();
        lenient().when(merchantAwardCommandService.create(any(pb.merchant_award.MerchantAwardCommand.CreateMerchantAwardRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt proto = pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(merchantAwardCommandService.trashedMerchantAward(any(pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt proto = pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(merchantAwardCommandService.restoreMerchantAward(any(pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
