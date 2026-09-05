package com.sanedge.merchant_award.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;
import com.sanedge.merchant_award.entity.MerchantCertificationAndAward;
import com.sanedge.merchant_award.service.MerchantAwardQueryService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantAwardQueryGrpcHandlerTest {

    @Mock
    private MerchantAwardQueryService merchantAwardQueryService;

    @InjectMocks
    private MerchantAwardQueryGrpcHandler handler;

    private MerchantCertificationAndAward createMockAward(Long id) {
        MerchantCertificationAndAward a = new MerchantCertificationAndAward();
        a.id = id;
        a.setMerchantId(1);
        a.setTitle("Award " + id);
        a.setDescription("Description " + id);
        a.setIssuedBy("Issuer " + id);
        a.setCertificateUrl("https://example.com/cert" + id + ".pdf");
        a.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        a.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return a;
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    void findAll_returnsMappedProto() {
        MerchantAwardResponse r1 = MerchantAwardResponse.from(createMockAward(1L));
        ApiResponsePagination<List<MerchantAwardResponse>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(r1), new PaginationMeta(1, 10, 1, 1));
        lenient().when(merchantAwardQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().item(resp));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().setPage(1).setPageSize(10).setSearch("").build();

        var result = handler.findAll(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getDataCount()).isEqualTo(1);
        assertThat(result.getPagination().getTotalRecords()).isEqualTo(1);
    }

    @Test
    void findAll_throwsStatusInternalOnFailure() {
        lenient().when(merchantAwardQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("db error")));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().build();

        try {
            handler.findAll(req).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        }
    }

    @Test
    void findById_returnsMappedProto() {
        MerchantAwardResponse r = MerchantAwardResponse.from(createMockAward(1L));
        ApiResponse<MerchantAwardResponse> resp = ApiResponse.success("ok", r);
        when(merchantAwardQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(resp));

        pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest req = pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest
                .newBuilder().setId(1).build();

        var result = handler.findById(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getData().getTitle()).isEqualTo("Award 1");
    }

    @Test
    void findById_throwsStatusNotFoundOnMissing() {
        when(merchantAwardQueryService.findById(anyLong()))
                .thenReturn(Uni.createFrom().failure(new com.sanedge.common.exception.ResourceNotFoundException("not found")));

        pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest req = pb.merchant_award.MerchantAwardCommon.FindByIdMerchantAwardRequest
                .newBuilder().setId(999).build();

        try {
            handler.findById(req).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }

    @Test
    void findByActive_returnsMappedProto() {
        MerchantAwardResponseDeleteAt r = MerchantAwardResponseDeleteAt.from(createMockAward(1L));
        ApiResponsePagination<List<MerchantAwardResponseDeleteAt>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(r), new PaginationMeta(1, 10, 1, 1));
        lenient().when(merchantAwardQueryService.findByActive(any()))
                .thenReturn(Uni.createFrom().item(resp));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().build();

        var result = handler.findByActive(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getDataCount()).isEqualTo(1);
    }

    @Test
    void findByTrashed_returnsMappedProto() {
        MerchantAwardResponseDeleteAt r = MerchantAwardResponseDeleteAt.from(createMockAward(1L));
        ApiResponsePagination<List<MerchantAwardResponseDeleteAt>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(r), new PaginationMeta(1, 10, 1, 1));
        lenient().when(merchantAwardQueryService.findByTrashed(any()))
                .thenReturn(Uni.createFrom().item(resp));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().build();

        var result = handler.findByTrashed(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getDataCount()).isEqualTo(1);
    }
}
