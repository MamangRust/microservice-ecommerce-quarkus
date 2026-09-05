package com.sanedge.merchant_award.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat sertifikasi atau penghargaan merchant")
public class CreateMerchantAwardRequest {

    @NotNull
    @Schema(description = "ID merchant yang memiliki sertifikasi/penghargaan", example = "123")
    private Integer merchantId;

    @NotBlank
    @Schema(description = "Judul sertifikasi/penghargaan", example = "ISO 9001 Certification")
    private String title;

    @NotBlank
    @Schema(description = "Deskripsi sertifikasi/penghargaan", example = "Sertifikasi kualitas ISO 9001 untuk manajemen kualitas")
    private String description;

    @NotBlank
    @Schema(description = "Dikeluarkan oleh", example = "ISO Organization")
    private String issuedBy;

    @NotBlank
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    @Schema(description = "Tanggal diterbitkan (YYYY-MM-DD)", example = "2024-01-01")
    private String issueDate;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}")
    @Schema(description = "Tanggal kadaluarsa (YYYY-MM-DD)", example = "2025-01-01", nullable = true)
    private String expiryDate;

    @Schema(description = "URL sertifikat", example = "https://example.com/certificate.pdf", nullable = true)
    private String certificateUrl;
}