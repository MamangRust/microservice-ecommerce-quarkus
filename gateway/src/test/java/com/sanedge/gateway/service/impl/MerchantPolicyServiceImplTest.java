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
class MerchantPolicyServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.merchant_policy.MutinyMerchantPolicyQueryServiceGrpc.MutinyMerchantPolicyQueryServiceStub merchantPolicyQueryService;
    @Mock
    private pb.merchant_policy.MutinyMerchantPolicyCommandServiceGrpc.MutinyMerchantPolicyCommandServiceStub merchantPolicyCommandService;

    private MerchantPolicyServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = MerchantPolicyServiceImpl.class.getDeclaredField(name);
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
        service = new MerchantPolicyServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("merchantPolicyQueryService", merchantPolicyQueryService);
        inject("merchantPolicyCommandService", merchantPolicyCommandService);
    }

    @Test
    void findById_PropagatesPolicyResponse() {
        pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies proto = pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(merchantPolicyQueryService.findById(any(pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesPolicyResponse() {
        pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies proto = pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies.newBuilder()
                .setStatus("success").setMessage("created").build();
        pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest req = pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest.newBuilder().build();
        lenient().when(merchantPolicyCommandService.create(any(pb.merchant_policy.MerchantPolicyCommand.CreateMerchantPoliciesRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void delete_TrashStub_Propagates() {
        pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt proto = pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(merchantPolicyCommandService.trashedMerchantPolicies(any(pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.delete(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt proto = pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(merchantPolicyCommandService.restoreMerchantPolicies(any(pb.merchant_policy.MerchantPolicyCommon.FindByIdMerchantPoliciesRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
