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
class MerchantBusinessServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.merchant_business.MutinyMerchantBusinessQueryServiceGrpc.MutinyMerchantBusinessQueryServiceStub merchantBusinessQueryService;
    @Mock
    private pb.merchant_business.MutinyMerchantBusinessCommandServiceGrpc.MutinyMerchantBusinessCommandServiceStub merchantBusinessCommandService;

    private MerchantBusinessServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = MerchantBusinessServiceImpl.class.getDeclaredField(name);
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
        service = new MerchantBusinessServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("merchantBusinessQueryService", merchantBusinessQueryService);
        inject("merchantBusinessCommandService", merchantBusinessCommandService);
    }

    @Test
    void findById_PropagatesBusinessResponse() {
        pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness proto = pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(merchantBusinessQueryService.findById(any(pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesBusinessResponse() {
        pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness proto = pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest req = pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest.newBuilder().build();
        lenient().when(merchantBusinessCommandService.create(any(pb.merchant_business.MerchantBusinessCommand.CreateMerchantBusinessRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt proto = pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(merchantBusinessCommandService.trashedMerchantBusiness(any(pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt proto = pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(merchantBusinessCommandService.restoreMerchantBusiness(any(pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
