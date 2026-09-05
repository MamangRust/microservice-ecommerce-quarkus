package com.sanedge.shipping_address.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponse;
import com.sanedge.shipping_address.domain.response.ShippingAddressResponseDeleteAt;
import com.sanedge.shipping_address.service.ShippingAddressCommand;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.shipping_address.ShippingAddressCommon;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShippingAddressCommandGrpcHandlerTest {

        @Mock
        private ShippingAddressCommand shippingAddressCommand;

        @Mock
        private ShippingAddressResponse shippingAddressResponse;

        @Mock
        private ShippingAddressResponseDeleteAt shippingAddressResponseDeleteAt;

        @Mock
        private ApiResponse<ShippingAddressResponse> apiResponseSuccess;

        @Mock
        private ApiResponse<ShippingAddressResponseDeleteAt> apiResponseDeleteAtSuccess;

        @Mock
        private ApiResponse<Void> apiResponseEmptySuccess;

        private ShippingAddressCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new ShippingAddressCommandGrpcHandler();
                handler.shippingAddressCommand = shippingAddressCommand;

                when(shippingAddressResponse.getId()).thenReturn(1L);
                when(shippingAddressResponse.getOrderId()).thenReturn(100);
                when(shippingAddressResponse.getAlamat()).thenReturn("Jl. Mocking");
                when(shippingAddressResponse.getProvinsi()).thenReturn("Jawa Tengah");
                when(shippingAddressResponse.getNegara()).thenReturn("Indonesia");
                when(shippingAddressResponse.getKota()).thenReturn("Semarang");
                when(shippingAddressResponse.getShippingMethod()).thenReturn("SICEPAT");
                when(shippingAddressResponse.getShippingCost()).thenReturn(20000);
                when(shippingAddressResponse.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
                when(shippingAddressResponse.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");

                when(shippingAddressResponseDeleteAt.getId()).thenReturn(1L);
                when(shippingAddressResponseDeleteAt.getOrderId()).thenReturn(100);
                when(shippingAddressResponseDeleteAt.getAlamat()).thenReturn("Jl. Mocking");
                when(shippingAddressResponseDeleteAt.getProvinsi()).thenReturn("Jawa Tengah");
                when(shippingAddressResponseDeleteAt.getNegara()).thenReturn("Indonesia");
                when(shippingAddressResponseDeleteAt.getKota()).thenReturn("Semarang");
                when(shippingAddressResponseDeleteAt.getShippingMethod()).thenReturn("SICEPAT");
                when(shippingAddressResponseDeleteAt.getShippingCost()).thenReturn(20000);
                when(shippingAddressResponseDeleteAt.getCreatedAt()).thenReturn("2024-01-01 00:00:00.0");
                when(shippingAddressResponseDeleteAt.getUpdatedAt()).thenReturn("2024-01-01 00:00:00.0");
                when(shippingAddressResponseDeleteAt.getDeletedAt()).thenReturn("2024-01-02 00:00:00.0");

                when(apiResponseSuccess.status()).thenReturn("success");
                when(apiResponseSuccess.message()).thenReturn("Operation successful");
                when(apiResponseSuccess.data()).thenReturn(shippingAddressResponse);

                when(apiResponseDeleteAtSuccess.status()).thenReturn("success");
                when(apiResponseDeleteAtSuccess.message()).thenReturn("Operation successful");
                when(apiResponseDeleteAtSuccess.data()).thenReturn(shippingAddressResponseDeleteAt);

                when(apiResponseEmptySuccess.status()).thenReturn("success");
                when(apiResponseEmptySuccess.message()).thenReturn("Bulk operation successful");
                when(apiResponseEmptySuccess.data()).thenReturn(null);
        }

        @Test
        @DisplayName("createShipping - should return ApiResponseShipping on success")
        void createShipping_Success() {
                pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest request = pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest
                                .newBuilder()
                                .setOrderId(100).setAlamat("Jl. Test").setProvinsi("DKI").setNegara("ID")
                                .setKota("Jakarta").setShippingMethod("JNE").setShippingCost(15000).build();

                when(shippingAddressCommand.create(any())).thenReturn(Uni.createFrom().item(apiResponseSuccess));

                ShippingAddressCommon.ApiResponseShipping response = handler.createShipping(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getKota()).isEqualTo("Semarang");
        }

        @Test
        @DisplayName("createShipping - should return INTERNAL on failure")
        void createShipping_InternalError() {
                pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest request = pb.shipping_address.ShippingAddressCommand.CreateShippingAddressRequest
                                .newBuilder().build();

                when(shippingAddressCommand.create(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.createShipping(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
                assertThat(ex.getStatus().getCode()).isEqualTo(Status.INTERNAL.getCode());
        }

        @Test
        @DisplayName("updateShipping - should return ApiResponseShipping on success")
        void updateShipping_Success() {
                pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest request = pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest
                                .newBuilder().setShippingId(1).setAlamat("Jl. Updated").build();

                when(shippingAddressCommand.update(any())).thenReturn(Uni.createFrom().item(apiResponseSuccess));

                ShippingAddressCommon.ApiResponseShipping response = handler.updateShipping(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("updateShipping - id=0 harus INVALID_ARGUMENT")
        void updateShipping_InvalidShippingId_ReturnsInvalidArgument() {
                pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest request = pb.shipping_address.ShippingAddressCommand.UpdateShippingAddressRequest
                                .newBuilder().setShippingId(0).setAlamat("Jl. Updated").build();

                assertThatThrownBy(() -> handler.updateShipping(request).await().indefinitely())
                                .isInstanceOf(StatusRuntimeException.class)
                                .satisfies(e -> {
                                        StatusRuntimeException sre = (StatusRuntimeException) e;
                                        assertThat(sre.getStatus().getCode()).isEqualTo(Status.INVALID_ARGUMENT.getCode());
                                        assertThat(sre.getStatus().getDescription()).contains("Shipping id");
                                });
        }

        @Test
        @DisplayName("trashedShipping - should return ApiResponseShippingDeleteAt on success")
        void trashedShipping_Success() {
                ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                                .newBuilder().setId(1).build();

                when(shippingAddressCommand.trash(any())).thenReturn(Uni.createFrom().item(apiResponseDeleteAtSuccess));

                ShippingAddressCommon.ApiResponseShippingDeleteAt response = handler.trashedShipping(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getDeletedAt().getValue()).isEqualTo("2024-01-02 00:00:00.0");
        }

        @Test
        @DisplayName("restoreShipping - should return ApiResponseShippingDeleteAt on success")
        void restoreShipping_Success() {
                ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                                .newBuilder().setId(1).build();

                when(shippingAddressCommand.restore(any()))
                                .thenReturn(Uni.createFrom().item(apiResponseDeleteAtSuccess));

                ShippingAddressCommon.ApiResponseShippingDeleteAt response = handler.restoreShipping(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("deleteShippingPermanent - should return ApiResponseShippingDelete on success")
        void deleteShippingPermanent_Success() {
                ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                                .newBuilder().setId(1).build();

                when(shippingAddressCommand.deletePermanently(any()))
                                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

                ShippingAddressCommon.ApiResponseShippingDelete response = handler.deleteShippingPermanent(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Bulk operation successful");
        }

        @Test
        @DisplayName("deleteShippingByOrderPermanent - should return ApiResponseShippingDelete on success")
        void deleteShippingByOrderPermanent_Success() {
                ShippingAddressCommon.FindByIdShippingRequest request = ShippingAddressCommon.FindByIdShippingRequest
                                .newBuilder().setId(100).build();

                when(shippingAddressCommand.deletePermanently(any()))
                                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

                ShippingAddressCommon.ApiResponseShippingDelete response = handler
                                .deleteShippingByOrderPermanent(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("restoreAllShipping - should return ApiResponseShippingAll on success")
        void restoreAllShipping_Success() {
                when(shippingAddressCommand.restoreAll()).thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

                ShippingAddressCommon.ApiResponseShippingAll response = handler
                                .restoreAllShipping(com.google.protobuf.Empty.getDefaultInstance()).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("deleteAllShippingPermanent - should return ApiResponseShippingAll on success")
        void deleteAllShippingPermanent_Success() {
                when(shippingAddressCommand.deleteAllPermanent())
                                .thenReturn(Uni.createFrom().item(apiResponseEmptySuccess));

                ShippingAddressCommon.ApiResponseShippingAll response = handler
                                .deleteAllShippingPermanent(com.google.protobuf.Empty.getDefaultInstance()).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }
}
