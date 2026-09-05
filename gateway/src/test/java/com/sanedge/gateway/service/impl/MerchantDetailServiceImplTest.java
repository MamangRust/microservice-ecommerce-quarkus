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
class MerchantDetailServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.merchant_detail.MutinyMerchantDetailQueryServiceGrpc.MutinyMerchantDetailQueryServiceStub merchantDetailQueryService;
    @Mock
    private pb.merchant_detail.MutinyMerchantDetailCommandServiceGrpc.MutinyMerchantDetailCommandServiceStub merchantDetailCommandService;

    private MerchantDetailServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = MerchantDetailServiceImpl.class.getDeclaredField(name);
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
        service = new MerchantDetailServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("merchantDetailQueryService", merchantDetailQueryService);
        inject("merchantDetailCommandService", merchantDetailCommandService);
    }

    @Test
    void findById_PropagatesDetailResponse() {
        pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail proto = pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(merchantDetailQueryService.findById(any(pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesDetailResponse() {
        pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail proto = pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest req = pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest.newBuilder().build();
        lenient().when(merchantDetailCommandService.create(any(pb.merchant_detail.MerchantDetailCommand.CreateMerchantDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt proto = pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(merchantDetailCommandService.trashedMerchantDetail(any(pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt proto = pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(merchantDetailCommandService.restoreMerchantDetail(any(pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
