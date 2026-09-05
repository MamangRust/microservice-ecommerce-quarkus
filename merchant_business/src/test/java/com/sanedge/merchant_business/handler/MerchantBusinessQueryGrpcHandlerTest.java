package com.sanedge.merchant_business.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.entity.MerchantBusinessInformation;
import com.sanedge.merchant_business.service.MerchantBusinessQueryService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantBusinessQueryGrpcHandlerTest {

    @Mock
    private MerchantBusinessQueryService service;

    @InjectMocks
    private MerchantBusinessQueryGrpcHandler handler;

    private MerchantBusinessInformation createMock(Long id) {
        MerchantBusinessInformation e = new MerchantBusinessInformation();
        e.id = id;
        e.setMerchantId(1);
        e.setBusinessType("Retail");
        e.setTaxId("12.345.678.9-012.345");
        e.setEstablishedYear(2020);
        e.setNumberOfEmployees(50);
        e.setWebsiteUrl("https://example.com");
        e.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        e.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return e;
    }

    @Test
    void findAll_returnsMappedProto() {
        MerchantBusinessResponse r1 = MerchantBusinessResponse.from(createMock(1L));
        ApiResponsePagination<List<MerchantBusinessResponse>> resp = new ApiResponsePagination<>(
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
        MerchantBusinessResponse r = MerchantBusinessResponse.from(createMock(1L));
        ApiResponse<MerchantBusinessResponse> resp = ApiResponse.success("ok", r);
        when(service.findById(anyLong())).thenReturn(Uni.createFrom().item(resp));

        pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest req = pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest
                .newBuilder().setId(1).build();

        var result = handler.findById(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getData().getTaxId()).isEqualTo("12.345.678.9-012.345");
    }

    @Test
    void findById_throwsStatusNotFoundOnMissing() {
        when(service.findById(anyLong()))
                .thenReturn(Uni.createFrom().failure(new com.sanedge.common.exception.ResourceNotFoundException("not found")));

        pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest req = pb.merchant_business.MerchantBusinessCommon.FindByIdMerchantBusinessRequest
                .newBuilder().setId(999).build();

        try {
            handler.findById(req).await().indefinitely();
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }

    @Test
    void findByActive_returnsMappedProto() {
        MerchantBusinessResponseDeleteAt r = MerchantBusinessResponseDeleteAt.from(createMock(1L));
        ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> resp = new ApiResponsePagination<>(
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
        MerchantBusinessResponseDeleteAt r = MerchantBusinessResponseDeleteAt.from(createMock(1L));
        ApiResponsePagination<List<MerchantBusinessResponseDeleteAt>> resp = new ApiResponsePagination<>(
                "success", "ok", List.of(r), new PaginationMeta(1, 10, 1, 1));
        lenient().when(service.findByTrashed(any())).thenReturn(Uni.createFrom().item(resp));

        pb.merchant.MerchantQuery.FindAllMerchantRequest req = pb.merchant.MerchantQuery.FindAllMerchantRequest
                .newBuilder().build();

        var result = handler.findByTrashed(req).await().indefinitely();

        assertThat(result.getStatus()).isEqualTo("success");
        assertThat(result.getDataCount()).isEqualTo(1);
    }
}
