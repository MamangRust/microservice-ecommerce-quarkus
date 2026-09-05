package com.sanedge.cart.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.cart.domain.requests.CreateCartRequest;
import com.sanedge.cart.domain.requests.DeleteCartRequest;
import com.sanedge.cart.domain.response.CartResponse;
import com.sanedge.cart.service.CartService;
import com.sanedge.common.domain.response.ApiResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.cart.CartCommand;
import pb.cart.CartCommon;

@ExtendWith(MockitoExtension.class)
class CartCommandGrpcHandlerTest {

        @Mock
        private CartService cartService;

        private CartCommandGrpcHandler handler;

        @BeforeEach
        void setUp() throws Exception {
                handler = new CartCommandGrpcHandler();
                injectService(handler, cartService);
        }

        private void injectService(Object target, Object service) throws Exception {
                Field field = target.getClass().getDeclaredField("cartService");
                field.setAccessible(true);
                field.set(target, service);
        }

        @Test
        @DisplayName("create - should return success response when cart created successfully")
        void create_Success() {
                CartCommand.CreateCartRequest request = CartCommand.CreateCartRequest.newBuilder()
                                .setUserId(1)
                                .setProductId(100)
                                .setQuantity(2)
                                .build();

                CartResponse mockResponse = CartResponse.builder()
                                .id(1L)
                                .userId(1)
                                .productId(100)
                                .name("Product 100")
                                .price(100)
                                .image("default.png")
                                .quantity(2)
                                .weight(100)
                                .build();

                ApiResponse<CartResponse> apiResponse = ApiResponse.success("Cart created successfully", mockResponse);

                when(cartService.createCart(any(CreateCartRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponseCart response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Cart created successfully");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getUserId()).isEqualTo(1);
                assertThat(response.getData().getProductId()).isEqualTo(100);
                assertThat(response.getData().getQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("create - should return INTERNAL error when exception thrown")
        void create_InternalError() {
                CartCommand.CreateCartRequest request = CartCommand.CreateCartRequest.newBuilder()
                                .setUserId(1)
                                .setProductId(100)
                                .setQuantity(2)
                                .build();

                when(cartService.createCart(any(CreateCartRequest.class)))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Database error")));

                StatusRuntimeException exception = null;
                try {
                        handler.create(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
                assertThat(exception.getStatus().getDescription()).isEqualTo("Database error");
        }

        @Test
        @DisplayName("delete - should return success response when cart deleted successfully")
        void delete_Success() {
                CartCommand.DeleteCartRequest request = CartCommand.DeleteCartRequest.newBuilder()
                                .setCartId(1)
                                .build();

                ApiResponse<Void> apiResponse = ApiResponse.success("Cart deleted permanently");

                when(cartService.deletePermanent(anyLong()))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponseCartDelete response = handler.delete(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Cart deleted permanently");
        }

        @Test
        @DisplayName("delete - should return NOT_FOUND when cart not found")
        void delete_NotFound() {
                CartCommand.DeleteCartRequest request = CartCommand.DeleteCartRequest.newBuilder()
                                .setCartId(999)
                                .build();

                when(cartService.deletePermanent(anyLong()))
                                .thenReturn(Uni.createFrom().failure(
                                                new com.sanedge.common.exception.ResourceNotFoundException(
                                                                "Cart not found with id: 999")));

                StatusRuntimeException exception = null;
                try {
                        handler.delete(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.NOT_FOUND.getCode());
        }

        @Test
        @DisplayName("deleteAll - should return success response when all carts deleted successfully")
        void deleteAll_Success() {
                CartCommand.DeleteAllCartRequest request = CartCommand.DeleteAllCartRequest.newBuilder()
                                .addCartIds(1)
                                .addCartIds(2)
                                .addCartIds(3)
                                .build();

                ApiResponse<Void> apiResponse = ApiResponse.success("Carts deleted permanently");

                when(cartService.deleteAllPermanently(any(DeleteCartRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponseCartAll response = handler.deleteAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Carts deleted permanently");
        }

        @Test
        @DisplayName("deleteAll - should return INTERNAL error when exception thrown")
        void deleteAll_InternalError() {
                CartCommand.DeleteAllCartRequest request = CartCommand.DeleteAllCartRequest.newBuilder()
                                .addCartIds(1)
                                .addCartIds(2)
                                .build();

                when(cartService.deleteAllPermanently(any(DeleteCartRequest.class)))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Failed to delete carts")));

                StatusRuntimeException exception = null;
                try {
                        handler.deleteAll(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        exception = e;
                }

                assertThat(exception).isNotNull();
                assertThat(exception.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
        }

        @Test
        @DisplayName("create - should correctly map request fields to domain object")
        void create_RequestMapping() {
                CartCommand.CreateCartRequest request = CartCommand.CreateCartRequest.newBuilder()
                                .setUserId(5)
                                .setProductId(200)
                                .setQuantity(3)
                                .build();

                CartResponse mockResponse = CartResponse.builder()
                                .id(1L)
                                .userId(5)
                                .productId(200)
                                .quantity(3)
                                .build();

                ApiResponse<CartResponse> apiResponse = ApiResponse.success("Cart created", mockResponse);

                when(cartService.createCart(any(CreateCartRequest.class)))
                                .thenAnswer(invocation -> {
                                        CreateCartRequest domainReq = invocation.getArgument(0);

                                        assertThat(domainReq.getUserId()).isEqualTo(5);
                                        assertThat(domainReq.getProductId()).isEqualTo(200);
                                        assertThat(domainReq.getQuantity()).isEqualTo(3);
                                        return Uni.createFrom().item(apiResponse);
                                });

                CartCommon.ApiResponseCart response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("delete - should correctly convert cartId to Long")
        void delete_CartIdMapping() {
                CartCommand.DeleteCartRequest request = CartCommand.DeleteCartRequest.newBuilder()
                                .setCartId(42)
                                .build();

                ApiResponse<Void> apiResponse = ApiResponse.success("Cart deleted permanently");

                when(cartService.deletePermanent(anyLong()))
                                .thenAnswer(invocation -> {
                                        Long cartId = invocation.getArgument(0);
                                        assertThat(cartId).isEqualTo(42L);
                                        return Uni.createFrom().item(apiResponse);
                                });

                CartCommon.ApiResponseCartDelete response = handler.delete(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("create - should correctly map null data in response")
        void create_NullData() {
                CartCommand.CreateCartRequest request = CartCommand.CreateCartRequest.newBuilder()
                                .setUserId(1)
                                .setProductId(100)
                                .setQuantity(2)
                                .build();

                ApiResponse<CartResponse> apiResponse = ApiResponse.<CartResponse>success("Cart created", null);

                when(cartService.createCart(any(CreateCartRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponseCart response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("create - should correctly map all cart fields in response")
        void create_AllFieldsMapped() {
                CartCommand.CreateCartRequest request = CartCommand.CreateCartRequest.newBuilder()
                                .setUserId(1)
                                .setProductId(100)
                                .setQuantity(2)
                                .build();

                CartResponse mockResponse = CartResponse.builder()
                                .id(1L)
                                .userId(1)
                                .productId(100)
                                .name("Test Product")
                                .price(500)
                                .image("test.jpg")
                                .quantity(2)
                                .weight(250)
                                .createdAt("2024-01-01")
                                .updatedAt("2024-01-02")
                                .build();

                ApiResponse<CartResponse> apiResponse = ApiResponse.success("Cart created successfully", mockResponse);

                when(cartService.createCart(any(CreateCartRequest.class)))
                                .thenReturn(Uni.createFrom().item(apiResponse));

                CartCommon.ApiResponseCart response = handler.create(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getUserId()).isEqualTo(1);
                assertThat(response.getData().getProductId()).isEqualTo(100);
                assertThat(response.getData().getName()).isEqualTo("Test Product");
                assertThat(response.getData().getPrice()).isEqualTo(500);
                assertThat(response.getData().getImage()).isEqualTo("test.jpg");
                assertThat(response.getData().getQuantity()).isEqualTo(2);
                assertThat(response.getData().getWeight()).isEqualTo(250);
                assertThat(response.getData().getCreatedAt()).isEqualTo("2024-01-01");
                assertThat(response.getData().getUpdatedAt()).isEqualTo("2024-01-02");
        }

        @Test
        @DisplayName("delete - should correctly handle delete with zero cartIds")
        void deleteAll_EmptyList() {
                CartCommand.DeleteAllCartRequest request = CartCommand.DeleteAllCartRequest.newBuilder()
                                .build();

                ApiResponse<Void> apiResponse = ApiResponse.success("Carts deleted permanently");

                when(cartService.deleteAllPermanently(any(DeleteCartRequest.class)))
                                .thenAnswer(invocation -> {
                                        DeleteCartRequest domainReq = invocation.getArgument(0);
                                        assertThat(domainReq.getCartIds()).isEmpty();
                                        return Uni.createFrom().item(apiResponse);
                                });

                CartCommon.ApiResponseCartAll response = handler.deleteAll(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }
}
