package com.sanedge.cart.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk menambahkan produk ke cart")
public class CreateCartRequest {
    @NotNull
    @Min(1)
    @Schema(description = "Jumlah produk", example = "2")
    private Integer quantity;

    @NotNull
    @Schema(description = "ID produk", example = "456")
    private Integer productId;

    @Schema(description = "ID pengguna (opsional, bisa diambil dari session)")
    private Integer userId;
}