package com.sanedge.merchant.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat merchant baru")
public class CreateMerchantRequest {

    @NotNull
    @Schema(description = "ID user pemilik merchant", example = "123")
    private Integer userId;

    @NotBlank
    @Schema(description = "Nama merchant", example = "Toko ABC")
    private String name;

    @NotBlank
    @Schema(description = "Deskripsi merchant", example = "Toko yang menjual peralatan elektronik")
    private String description;

    @NotBlank
    @Schema(description = "Alamat merchant", example = "Jl. Sudirman No. 10")
    private String address;

    @NotBlank
    @Email
    @Schema(description = "Email kontak merchant", example = "contact@tokoabc.com")
    private String contactEmail;

    @NotBlank
    @Schema(description = "Nomor telepon merchant", example = "081234567890")
    private String contactPhone;

    @NotBlank
    @Schema(description = "Status merchant", example = "ACTIVE")
    private String status;
}