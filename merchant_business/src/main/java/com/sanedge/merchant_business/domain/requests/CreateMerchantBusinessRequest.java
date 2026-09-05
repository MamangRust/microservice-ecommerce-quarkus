package com.sanedge.merchant_business.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat informasi bisnis merchant")
public class CreateMerchantBusinessRequest {

    @NotNull
    @Schema(description = "ID merchant", example = "123")
    private Integer merchantId;

    @NotBlank
    @Schema(description = "Jenis usaha", example = "Retail")
    private String businessType;

    @NotBlank
    @Schema(description = "Nomor NPWP / Tax ID", example = "12.345.678.9-012.345")
    private String taxId;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Schema(description = "Tahun berdiri", example = "2020")
    private Integer establishedYear;

    @NotNull
    @Min(1)
    @Schema(description = "Jumlah karyawan", example = "50")
    private Integer numberOfEmployees;

    @Schema(description = "Website merchant", example = "https://example.com", nullable = true)
    private String websiteUrl;
}