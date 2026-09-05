package com.sanedge.shipping_address.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponseDeleteAt;
import com.sanedge.shipping_address.service.ShippingAddressQueryService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.shipping_address.ShippingAddressCommon;
import pb.shipping_address.ShippingAddressQuery;

@ExtendWith(MockitoExtension.class)
class ShippingAddressQueryGrpcHandlerTest {

    @Mock
    private ShippingAddressQueryService shippingAddressQueryService;

    @Mock
    private ShippingAddressResponse shippingAddressResponse;

    @Mock
    private ShippingAddressResponseDeleteAt shippingAddressResponseDeleteAt;

    @Mock
    private PaginationMeta paginationMeta;

    @Mock
    private ApiResponsePagination<List<ShippingAddressResponse>> paginationResponse;

    @Mock
    private ApiResponsePagination<List<ShippingAddressResponseDeleteAt>> paginationDeleteAtResponse;

    @Mock
    private ApiResponse<ShippingAddressResponse> singleResponse;

    private ShippingAddressQueryGrpcHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ShippingAddressQueryGrpcHandler();
        handler.shippingAddressQueryService = shippingAddressQueryService;

    }

    @Test
    @DisplayName("findAll - should return ApiResponsePaginationShipping on success")
    void findAll_Success() {

        setupShippingAddressResponse();
        setupPaginationMeta();
        when(paginationResponse.status()).thenReturn("success");
        when(paginationResponse.message()).thenReturn("Shipping addresses retrieved successfully");
        when(paginationResponse.data()).thenReturn(List.of(shippingAddressResponse));
        when(paginationResponse.pagination()).thenReturn(paginationMeta);

        ShippingAddressQuery.FindAllShippingRequest request = ShippingAddressQuery.FindAllShippingRequest.newBuilder()
                .setPage(1).setPageSize(10).setSearch("").build();

        when(shippingAddressQueryService.findAll(any())).thenReturn(Uni.createFrom().item(paginationResponse));

        ShippingAddressCommon.ApiResponsePaginationShipping response = handler.findAll(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getPagination().getTotalRecords()).isEqualTo(1);
        assertThat(response.getData(0).getShippingMethod()).isEqualTo("JNE");
    }

    @Test
    @DisplayName("findAll - should return INTERNAL on failure")
    void findAll_InternalError() {
        ShippingAddressQuery.FindAllShippingRequest request = ShippingAddressQuery.FindAllShippingRequest.newBuilder()
                .setPage(1).setPageSize(10).build();

        when(shippingAddressQueryService.findAll(any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

        StatusRuntimeException ex = null;
        try {
            handler.findAll(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
    }

    @Test
    @DisplayName("findById - should return ApiResponseShipping on success")
    void findById_Success() {

        setupShippingAddressResponse();
        when(singleResponse.status()).thenReturn("success");
        when(singleResponse.message()).thenReturn("Shipping address retrieved successfully");
        when(singleResponse.data()).thenReturn(shippingAddressResponse);

        ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                .newBuilder().setId(1).build();

        when(shippingAddressQueryService.findById(any())).thenReturn(Uni.createFrom().item(singleResponse));

        ShippingAddressCommon.ApiResponseShipping response = handler.findById(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getAlamat()).isEqualTo("Jl. Test No. 1");
        assertThat(response.getData().getShippingCost()).isEqualTo(15000);
    }

    @Test
    @DisplayName("findById - should return NOT_FOUND when not found")
    void findById_NotFound() {
        ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                .newBuilder().setId(999).build();

        when(shippingAddressQueryService.findById(any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Shipping address not found")));

        StatusRuntimeException ex = null;
        try {
            handler.findById(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("findByOrder - should return ApiResponseShipping on success")
    void findByOrder_Success() {

        setupShippingAddressResponse();
        when(singleResponse.status()).thenReturn("success");
        when(singleResponse.message()).thenReturn("Shipping address retrieved successfully");
        when(singleResponse.data()).thenReturn(shippingAddressResponse);

        ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                .newBuilder().setId(100).build();

        when(shippingAddressQueryService.findByOrder(any())).thenReturn(Uni.createFrom().item(singleResponse));

        ShippingAddressCommon.ApiResponseShipping response = handler.findByOrder(request).await().indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getOrderId()).isEqualTo(100);
    }

    @Test
    @DisplayName("findByOrder - should return NOT_FOUND when not found")
    void findByOrder_NotFound() {
        ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                .newBuilder().setId(999).build();

        when(shippingAddressQueryService.findByOrder(any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Order not found")));

        StatusRuntimeException ex = null;
        try {
            handler.findByOrder(request).await().indefinitely();
        } catch (StatusRuntimeException e) {
            ex = e;
        }

        assertThat(ex).isNotNull();
        assertThat(ex.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("findByActive - should return ApiResponsePaginationShippingDeleteAt on success")
    void findByActive_Success() {

        setupShippingAddressResponseDeleteAt();
        setupPaginationMeta();
        when(paginationDeleteAtResponse.status()).thenReturn("success");
        when(paginationDeleteAtResponse.message()).thenReturn("Trashed shipping addresses retrieved successfully");
        when(paginationDeleteAtResponse.data()).thenReturn(List.of(shippingAddressResponseDeleteAt));
        when(paginationDeleteAtResponse.pagination()).thenReturn(paginationMeta);

        ShippingAddressQuery.FindAllShippingRequest request = ShippingAddressQuery.FindAllShippingRequest.newBuilder()
                .setPage(1).setPageSize(10).build();

        when(shippingAddressQueryService.findByActive(any()))
                .thenReturn(Uni.createFrom().item(paginationDeleteAtResponse));

        ShippingAddressCommon.ApiResponsePaginationShippingDeleteAt response = handler.findByActive(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getDataCount()).isEqualTo(1);
        assertThat(response.getData(0).getKota()).isEqualTo("Bandung");
    }

    @Test
    @DisplayName("findByTrashed - should return ApiResponsePaginationShippingDeleteAt on success")
    void findByTrashed_Success() {

        setupShippingAddressResponseDeleteAt();
        setupPaginationMeta();
        when(paginationDeleteAtResponse.status()).thenReturn("success");
        when(paginationDeleteAtResponse.message()).thenReturn("Trashed shipping addresses retrieved successfully");
        when(paginationDeleteAtResponse.data()).thenReturn(List.of(shippingAddressResponseDeleteAt));
        when(paginationDeleteAtResponse.pagination()).thenReturn(paginationMeta);

        ShippingAddressQuery.FindAllShippingRequest request = ShippingAddressQuery.FindAllShippingRequest.newBuilder()
                .setPage(1).setPageSize(10).build();

        when(shippingAddressQueryService.findByTrashed(any()))
                .thenReturn(Uni.createFrom().item(paginationDeleteAtResponse));

        ShippingAddressCommon.ApiResponsePaginationShippingDeleteAt response = handler.findByTrashed(request).await()
                .indefinitely();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData(0).getDeletedAt().getValue()).isEqualTo("2024-01-02 00:00:00.0");
    }

    private void setupShippingAddressResponse() {
        when(shippingAddressResponse.getId()).thenReturn(1L);
        when(shippingAddressResponse.getOrderId()).thenReturn(100);
        when(shippingAddressResponse.getAlamat()).thenReturn("Jl. Test No. 1");
        when(shippingAddressResponse.getProvinsi()).thenReturn("DKI Jakarta");
        when(shippingAddressResponse.getNegara()).thenReturn("Indonesia");
        when(shippingAddressResponse.getKota()).thenReturn("Jakarta Selatan");
        when(shippingAddressResponse.getShippingMethod()).thenReturn("JNE");
        when(shippingAddressResponse.getShippingCost()).thenReturn(15000);
        when(shippingAddressResponse.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(shippingAddressResponse.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");
    }

    private void setupShippingAddressResponseDeleteAt() {
        when(shippingAddressResponseDeleteAt.getId()).thenReturn(2L);
        when(shippingAddressResponseDeleteAt.getOrderId()).thenReturn(101);
        when(shippingAddressResponseDeleteAt.getAlamat()).thenReturn("Jl. Trash No. 2");
        when(shippingAddressResponseDeleteAt.getProvinsi()).thenReturn("Jawa Barat");
        when(shippingAddressResponseDeleteAt.getNegara()).thenReturn("Indonesia");
        when(shippingAddressResponseDeleteAt.getKota()).thenReturn("Bandung");
        when(shippingAddressResponseDeleteAt.getShippingMethod()).thenReturn("JNT");
        when(shippingAddressResponseDeleteAt.getShippingCost()).thenReturn(10000);
        when(shippingAddressResponseDeleteAt.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(shippingAddressResponseDeleteAt.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");
        when(shippingAddressResponseDeleteAt.getDeletedAt()).thenReturn("2024-01-02 00:00:00.0");
    }

    private void setupPaginationMeta() {
        when(paginationMeta.currentPage()).thenReturn(1);
        when(paginationMeta.pageSize()).thenReturn(10);
        when(paginationMeta.totalPages()).thenReturn(1);
        when(paginationMeta.totalRecords()).thenReturn(1);
    }
}
