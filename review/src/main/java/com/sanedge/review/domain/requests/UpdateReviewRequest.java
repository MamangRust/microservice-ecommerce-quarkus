package com.sanedge.review.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk memperbarui review yang sudah ada")
public class UpdateReviewRequest {

    @NotNull(message = "ID review wajib diisi")
    @Schema(description = "ID review", example = "202", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reviewId;

    @NotBlank(message = "Nama pengulas wajib diisi")
    @Schema(description = "Nama pengulas", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Min(value = 1, message = "Rating harus antara 1 sampai 5")
    @Max(value = 5, message = "Rating harus antara 1 sampai 5")
    @Schema(description = "Rating (1-5)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private int rating;

    @NotBlank(message = "Komentar wajib diisi")
    @Schema(description = "Komentar review", example = "Komentar diperbarui", requiredMode = Schema.RequiredMode.REQUIRED)
    private String comment;
}
