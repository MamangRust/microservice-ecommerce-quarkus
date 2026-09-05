package com.sanedge.merchant_policy.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
@Schema(description = "Request untuk memperbarui policy merchant")
public class UpdateMerchantPolicyRequest {

    @NotNull
    @Schema(description = "ID policy merchant yang akan diperbarui", example = "1")
    private Integer merchantPolicyId;

    @NotBlank
    @Schema(description = "Tipe policy", example = "Refund")
    private String policyType;

    @NotBlank
    @Schema(description = "Judul policy", example = "Refund Policy")
    private String title;

    @NotBlank
    @Schema(description = "Deskripsi policy", example = "Kebijakan pengembalian barang dalam 7 hari")
    private String description;
}