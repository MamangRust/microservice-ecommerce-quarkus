package com.sanedge.shipping_address.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk memperbarui alamat pengiriman")
public class UpdateShippingAddressRequest {

    @NotNull
    @Schema(description = "ID alamat pengiriman", example = "1")
    private Integer shippingId;

    @Schema(description = "ID order, optional")
    private Integer orderId;

    @Schema(description = "Alamat lengkap", example = "Jl. Merdeka No.1")
    private String alamat;

    @Schema(description = "Provinsi", example = "DKI Jakarta")
    private String provinsi;

    @Schema(description = "Kota", example = "Jakarta Pusat")
    private String kota;

    @NotBlank
    @Schema(description = "Kurir", example = "JNE")
    private String courier;

    @NotBlank
    @Schema(description = "Metode pengiriman", example = "REG")
    private String shippingMethod;

    @NotNull
    @Min(0)
    @Schema(description = "Biaya pengiriman", example = "15000")
    private Integer shippingCost;

    @Schema(description = "Negara", example = "Indonesia")
    private String negara;
}