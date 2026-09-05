package com.sanedge.cart.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.cart.domain.requests.FindAllCartsRequest;
import com.sanedge.cart.domain.response.CartResponse;
import com.sanedge.cart.service.CartService;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.cart.CartQuery;
import pb.cart.CartCommon;

@ExtendWith(MockitoExtension.class)
class CartQueryGrpcHandlerTest {

        @Mock
        private CartService cartService;

        private CartQueryGrpcHandler handler;

        @BeforeEach
        void setUp() throws Exception {
                handler = new CartQueryGrpcHandler();
                injectService(handler, cartService);
        }

        private void injectService(Object target, Object service) throws Exception {
                Field field = target.getClass().getDeclaredField("cartService");
                field.setAccessible(true);
                field.set(target, service);
        }

        @Test
        @DisplayName("findAll - should return success response with paginated carts")
        void findAll_Success() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(1)
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                CartResponse cart1 = CartResponse.builder()
                                .id(1L)
                                .userId(1)
                                .productId(100)
                                .name("Product 100")
                                .price(100)
                                .image("default.png")
                                .quantity(2)
                                .weight(100)
                                .build();

                CartResponse cart2 = CartResponse.builder()
                                .id(2L)
                                .userId(1)
                                .productId(101)
                                .name("Product 101")
                                .price(200)
                                .image("default.png")
                                .quantity(1)
                                .weight(150)
                                .build();

                PaginationMeta pagination = new PaginationMeta(1, 10, 1, 2);

                ApiResponsePagination<List<CartResponse>> apiResponse = new ApiResponsePagination<>(
                                "success", "Cart data fetched successfully", List.of(cart1, cart2), pagination);

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponsePaginationCart response = handler.findAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Cart data fetched successfully");
                assertThat(response.getDataList()).hasSize(2);
                assertThat(response.getData(0).getId()).isEqualTo(1);
                assertThat(response.getData(0).getProductId()).isEqualTo(100);
                assertThat(response.getData(1).getId()).isEqualTo(2);
                assertThat(response.getData(1).getProductId()).isEqualTo(101);
                assertThat(response.hasPagination()).isTrue();
                assertThat(response.getPagination().getCurrentPage()).isEqualTo(1);
                assertThat(response.getPagination().getPageSize()).isEqualTo(10);
                assertThat(response.getPagination().getTotalPages()).isEqualTo(1);
                assertThat(response.getPagination().getTotalRecords()).isEqualTo(2);
        }

        @Test
        @DisplayName("findAll - should return INTERNAL error when exception thrown")
        void findAll_InternalError() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(1)
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

                StatusRuntimeException exception = null;
                try {
                        handler.findAll(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
                assertThat(exception.getStatus().getDescription()).isEqualTo("Database error");
        }

        @Test
        @DisplayName("findAll - should correctly map request fields to domain object")
        void findAll_RequestMapping() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(5)
                                .setPage(2)
                                .setPageSize(20)
                                .setSearch("test")
                                .build();

                PaginationMeta pagination = new PaginationMeta(2, 20, 0, 0);

                ApiResponsePagination<List<CartResponse>> apiResponse = new ApiResponsePagination<>(
                                "success", "Cart data fetched successfully", List.of(), pagination);

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenAnswer(invocation -> {
                                        FindAllCartsRequest domainReq = invocation.getArgument(0);
                                        assertThat(domainReq.getUserId()).isEqualTo(5);
                                        assertThat(domainReq.getPage()).isEqualTo(2);
                                        assertThat(domainReq.getPageSize()).isEqualTo(20);
                                        assertThat(domainReq.getSearch()).isEqualTo("test");
                                        return Uni.createFrom().item(apiResponse);
                                });

                CartCommon.ApiResponsePaginationCart response = handler.findAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("findAll - should handle empty search query")
        void findAll_EmptySearch() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(1)
                                .setPage(1)
                                .setPageSize(10)
                                .setSearch("")
                                .build();

                PaginationMeta pagination = new PaginationMeta(1, 10, 0, 0);

                ApiResponsePagination<List<CartResponse>> apiResponse = new ApiResponsePagination<>(
                                "success", "Cart data fetched successfully", List.of(), pagination);

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenAnswer(invocation -> {
                                        FindAllCartsRequest domainReq = invocation.getArgument(0);
                                        assertThat(domainReq.getSearch()).isEmpty();
                                        return Uni.createFrom().item(apiResponse);
                                });

                CartCommon.ApiResponsePaginationCart response = handler.findAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("findAll - should correctly map empty cart list")
        void findAll_EmptyList() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(1)
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                PaginationMeta pagination = new PaginationMeta(1, 10, 0, 0);

                ApiResponsePagination<List<CartResponse>> apiResponse = new ApiResponsePagination<>(
                                "success", "Cart data fetched successfully", List.of(), pagination);

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponsePaginationCart response = handler.findAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataList()).isEmpty();
        }

        @Test
        @DisplayName("findAll - should correctly map null pagination")
        void findAll_NullPagination() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(1)
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                CartResponse cart = CartResponse.builder()
                                .id(1L)
                                .userId(1)
                                .productId(100)
                                .name("Product 100")
                                .build();

                ApiResponsePagination<List<CartResponse>> apiResponse = new ApiResponsePagination<>(
                                "success", "Cart data fetched successfully", List.of(cart), null);

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponsePaginationCart response = handler.findAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.hasPagination()).isFalse();
        }

        @Test
        @DisplayName("findAll - should correctly map all cart fields in response")
        void findAll_AllFieldsMapped() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(1)
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                CartResponse mockCart = CartResponse.builder()
                                .id(1L)
                                .userId(1)
                                .productId(100)
                                .name("Full Product")
                                .price(500)
                                .image("full.jpg")
                                .quantity(5)
                                .weight(300)
                                .createdAt("2024-01-01")
                                .updatedAt("2024-01-02")
                                .build();

                PaginationMeta pagination = new PaginationMeta(1, 10, 1, 1);

                ApiResponsePagination<List<CartResponse>> apiResponse = new ApiResponsePagination<>(
                                "success", "Cart data fetched successfully", List.of(mockCart), pagination);

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponsePaginationCart response = handler.findAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getDataList()).hasSize(1);
                var cart = response.getData(0);
                assertThat(cart.getId()).isEqualTo(1);
                assertThat(cart.getUserId()).isEqualTo(1);
                assertThat(cart.getProductId()).isEqualTo(100);
                assertThat(cart.getName()).isEqualTo("Full Product");
                assertThat(cart.getPrice()).isEqualTo(500);
                assertThat(cart.getImage()).isEqualTo("full.jpg");
                assertThat(cart.getQuantity()).isEqualTo(5);
                assertThat(cart.getWeight()).isEqualTo(300);
                assertThat(cart.getCreatedAt()).isEqualTo("2024-01-01");
                assertThat(cart.getUpdatedAt()).isEqualTo("2024-01-02");
        }

        @Test
        @DisplayName("findAll - should correctly map pagination metadata")
        void findAll_PaginationMapping() {
                CartQuery.FindAllCartRequest request = CartQuery.FindAllCartRequest.newBuilder()
                                .setUserId(1)
                                .setPage(3)
                                .setPageSize(25)
                                .build();

                CartResponse cart = CartResponse.builder()
                                .id(1L)
                                .userId(1)
                                .productId(100)
                                .build();

                PaginationMeta pagination = new PaginationMeta(3, 25, 5, 100);

                ApiResponsePagination<List<CartResponse>> apiResponse = new ApiResponsePagination<>(
                                "success", "Cart data fetched successfully", List.of(cart), pagination);

                when(cartService.findAll(any(FindAllCartsRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponsePaginationCart response = handler.findAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.hasPagination()).isTrue();
                assertThat(response.getPagination().getCurrentPage()).isEqualTo(3);
                assertThat(response.getPagination().getPageSize()).isEqualTo(25);
                assertThat(response.getPagination().getTotalPages()).isEqualTo(5);
                assertThat(response.getPagination().getTotalRecords()).isEqualTo(100);
        }
}
