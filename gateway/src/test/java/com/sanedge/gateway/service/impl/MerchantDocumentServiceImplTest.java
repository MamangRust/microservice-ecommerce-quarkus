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

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentServiceImplTest {

    @Mock
    private TelemetryHelper telemetryHelper;
    @Mock
    private pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc.MutinyMerchantDocumentQueryServiceStub merchantDocumentQueryService;
    @Mock
    private pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc.MutinyMerchantDocumentCommandServiceStub merchantDocumentCommandService;

    private MerchantDocumentServiceImpl service;

    private void inject(String name, Object value) throws Exception {
        Field f = MerchantDocumentServiceImpl.class.getDeclaredField(name);
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
        service = new MerchantDocumentServiceImpl();
        inject("telemetryHelper", telemetryHelper);
        inject("merchantDocumentQueryService", merchantDocumentQueryService);
        inject("merchantDocumentCommandService", merchantDocumentCommandService);
    }

    @Test
    void findById_PropagatesDocumentResponse() {
        pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto = pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument.newBuilder()
                .setStatus("success").setMessage("ok").build();
        lenient().when(merchantDocumentQueryService.findById(any(pb.merchant_document.MerchantDocumentQuery.FindMerchantDocumentByIdRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.findById(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void create_PropagatesDocumentResponse() {
        pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto = pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument.newBuilder()
                .setStatus("success").setMessage("created").build();
        MerchantDocumentDto.CreateRequest req = new MerchantDocumentDto.CreateRequest(1, "PDF", "https://x");
        lenient().when(merchantDocumentCommandService.create(any(pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.create(req).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void trash_PropagatesDocumentResponse() {
        pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto = pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument.newBuilder()
                .setStatus("success").setMessage("trashed").build();
        lenient().when(merchantDocumentCommandService.trashed(any(pb.merchant_document.MerchantDocumentCommand.TrashedMerchantDocumentRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.trash(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("trashed");
    }
    @Test
    void restore_RestoreStub_Propagates() {
        pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto = pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument.newBuilder()
                .setStatus("success").setMessage("restored").build();
        lenient().when(merchantDocumentCommandService.restore(any(pb.merchant_document.MerchantDocumentCommand.RestoreMerchantDocumentRequest.class)))
                .thenAnswer(inv -> Uni.createFrom().item(proto));

        var result = service.restore(1).await().indefinitely();
        assertThat(result.message()).isEqualTo("restored");
    }

}
