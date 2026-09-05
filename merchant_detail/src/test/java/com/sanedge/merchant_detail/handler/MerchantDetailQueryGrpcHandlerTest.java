package com.sanedge.merchant_detail.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponse;
import com.sanedge.merchant_detail.domain.response.MerchantDetailRelationResponseDeleteAt;
import com.sanedge.merchant_detail.entity.MerchantDetailsRelation;
import com.sanedge.merchant_detail.service.MerchantDetailQueryService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantDetailQueryGrpcHandlerTest {

    @Mock
    private MerchantDetailQueryService service;

    @InjectMocks
    private MerchantDetailQueryGrpcHandler handler;

    private MerchantDetailsRelation createMock(Long id) {
        MerchantDetailsRelation rel = new MerchantDetailsRelation();
        rel.setId(id.intValue());
        rel.setMerchantId(1);
        rel.setDisplayName("Display " + id);
        rel.setCoverImageUrl("https://example.com/cover" + id + ".jpg");
        rel.setLogoUrl("https://example.com/logo" + id + ".jpg");
        rel.setShortDescription("Short desc " + id);
        rel.setWebsiteUrl("https://example.com");
        rel.setSocialMediaLinks(Collections.emptyList());
        rel.setCreatedAt("2024-01-01T00:00:00");
        rel.setUpdatedAt("2024-01-01T00:00:00");
        return rel;
    }

    @Test
    void findAll_returnsMappedProto() {
        MerchantDetailRelationResponse r1 = MerchantDetailRelationResponse.from(createMock(1L));
        ApiResponsePagination<List<MerchantDetailRelationResponse>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(r1), new PaginationMeta(1, 10, 1, 1));
        lenient().when(service.findAll(any())).thenReturn(Uni.createFrom().item(resp));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().setPage(1).setPageSize(10).setSearch("").build();

        var result = handler.findAll(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getDataCount()).isEqualTo(1);
        assertThat(result.getPagination().getTotalRecords()).isEqualTo(1);
    }

    @Test
    void findAll_throwsStatusInternalOnFailure() {
        lenient().when(service.findAll(any()))
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
        MerchantDetailRelationResponse r = MerchantDetailRelationResponse.from(createMock(1L));
        ApiResponse<MerchantDetailRelationResponse> resp = ApiResponse.success("ok", r);
        when(service.findById(anyLong())).thenReturn(Uni.createFrom().item(resp));

        pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest req = pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest
                .newBuilder().setId(1).build();

        var result = handler.findById(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getData().getDisplayName()).isEqualTo("Display 1");
    }

    @Test
    void findById_throwsStatusNotFoundOnMissing() {
        when(service.findById(anyLong()))
                .thenReturn(Uni.createFrom()
                        .failure(new com.sanedge.common.exception.ResourceNotFoundException("not found")));

        pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest req = pb.merchant_detail.MerchantDetailCommon.FindByIdMerchantDetailRequest
                .newBuilder().setId(999).build();

        try {
            handler.findById(req).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }

    @Test
    void findByActive_returnsMappedProto() {
        MerchantDetailRelationResponseDeleteAt r = MerchantDetailRelationResponseDeleteAt.from(createMock(1L));
        ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(r), new PaginationMeta(1, 10, 1, 1));
        lenient().when(service.findByActive(any())).thenReturn(Uni.createFrom().item(resp));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().build();

        var result = handler.findByActive(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getDataCount()).isEqualTo(1);
    }

    @Test
    void findByTrashed_returnsMappedProto() {
        MerchantDetailRelationResponseDeleteAt r = MerchantDetailRelationResponseDeleteAt.from(createMock(1L));
        ApiResponsePagination<List<MerchantDetailRelationResponseDeleteAt>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(r), new PaginationMeta(1, 10, 1, 1));
        lenient().when(service.findByTrashed(any())).thenReturn(Uni.createFrom().item(resp));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().build();

        var result = handler.findByTrashed(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getDataCount()).isEqualTo(1);
    }
}
