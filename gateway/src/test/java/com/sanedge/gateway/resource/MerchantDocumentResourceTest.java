package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.MerchantDocumentDto;
import com.sanedge.gateway.service.MerchantDocumentService;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentResourceTest {
    @Mock
    MerchantDocumentService merchantDocumentService;
    private MerchantDocumentResource merchantDocumentResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantDocumentResource = new MerchantDocumentResource();
        Field f = MerchantDocumentResource.class.getDeclaredField("merchantDocumentService");
        f.setAccessible(true);
        f.set(merchantDocumentResource, merchantDocumentService);
    }

    private MerchantDocumentDto.MerchantDocumentResponse mk(int docId) {
        return new MerchantDocumentDto.MerchantDocumentResponse(docId, 1, "PDF", "https://x", "active", "n", "", "");
    }

    @Test
    void listMerchantDocuments_Success() {
        MerchantDocumentDto.ApiResponsePaginationDocument dto = new MerchantDocumentDto.ApiResponsePaginationDocument(
                List.of(mk(1)), "success", "ok");
        lenient().when(merchantDocumentService.findAll(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.listMerchantDocuments(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void listActiveMerchantDocuments_Success() {
        MerchantDocumentDto.ApiResponsePaginationDocument dto = new MerchantDocumentDto.ApiResponsePaginationDocument(
                List.of(), "success", "ok");
        lenient().when(merchantDocumentService.findAllActive(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.listActiveMerchantDocuments(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void listTrashedMerchantDocuments_Success() {
        MerchantDocumentDto.ApiResponsePaginationDocument dto = new MerchantDocumentDto.ApiResponsePaginationDocument(
                List.of(), "success", "ok");
        lenient().when(merchantDocumentService.findAllTrashed(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.listTrashedMerchantDocuments(1, 10, "").await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void getMerchantDocument_Success() {
        MerchantDocumentDto.ApiResponseDocument dto = new MerchantDocumentDto.ApiResponseDocument(mk(1), "success", "ok");
        lenient().when(merchantDocumentService.findById(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.getMerchantDocument(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void createMerchantDocument_Success_Returns201() {
        MerchantDocumentDto.ApiResponseDocument dto = new MerchantDocumentDto.ApiResponseDocument(mk(1), "success", "ok");
        lenient().when(merchantDocumentService.create(any())).thenReturn(Uni.createFrom().item(dto));
        MerchantDocumentDto.CreateRequest req = new MerchantDocumentDto.CreateRequest(1, "PDF", "https://x");
        Response r = merchantDocumentResource.createMerchantDocument(req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(201);
    }

    @Test
    void updateMerchantDocument_Success() {
        MerchantDocumentDto.ApiResponseDocument dto = new MerchantDocumentDto.ApiResponseDocument(mk(1), "success", "ok");
        lenient().when(merchantDocumentService.update(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        MerchantDocumentDto.UpdateRequest req = new MerchantDocumentDto.UpdateRequest(1, "PDF", "https://x", "n", "active");
        Response r = merchantDocumentResource.updateMerchantDocument(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void updateMerchantDocumentStatus_Success() {
        MerchantDocumentDto.ApiResponseDocument dto = new MerchantDocumentDto.ApiResponseDocument(mk(1), "success", "ok");
        lenient().when(merchantDocumentService.updateStatus(anyInt(), any())).thenReturn(Uni.createFrom().item(dto));
        MerchantDocumentDto.UpdateStatusRequest req = new MerchantDocumentDto.UpdateStatusRequest(1, "n", "active");
        Response r = merchantDocumentResource.updateMerchantDocumentStatus(1, req).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void trashMerchantDocument_Success() {
        MerchantDocumentDto.ApiResponseDocument dto = new MerchantDocumentDto.ApiResponseDocument(mk(1), "success", "ok");
        lenient().when(merchantDocumentService.trash(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.trashMerchantDocument(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreMerchantDocument_Success() {
        MerchantDocumentDto.ApiResponseDocument dto = new MerchantDocumentDto.ApiResponseDocument(mk(1), "success", "ok");
        lenient().when(merchantDocumentService.restore(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.restoreMerchantDocument(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteMerchantDocumentPermanent_Success() {
        MerchantDocumentDto.SimpleResponse dto = new MerchantDocumentDto.SimpleResponse("success", "ok");
        lenient().when(merchantDocumentService.deletePermanent(anyInt())).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.deleteMerchantDocumentPermanent(1).await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void restoreAllMerchantDocuments_Success() {
        MerchantDocumentDto.SimpleResponse dto = new MerchantDocumentDto.SimpleResponse("success", "ok");
        lenient().when(merchantDocumentService.restoreAll()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.restoreAllMerchantDocuments().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }

    @Test
    void deleteAllMerchantDocuments_Success() {
        MerchantDocumentDto.SimpleResponse dto = new MerchantDocumentDto.SimpleResponse("success", "ok");
        lenient().when(merchantDocumentService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(dto));
        Response r = merchantDocumentResource.deleteAllMerchantDocuments().await().indefinitely();
        assertThat(r.getStatus()).isEqualTo(200);
    }
}
