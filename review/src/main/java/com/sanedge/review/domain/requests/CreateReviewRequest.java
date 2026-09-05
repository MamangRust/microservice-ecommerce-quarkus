package com.sanedge.review.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat review baru")
public class CreateReviewRequest {

    @Schema(description = "ID pengguna", example = "789", requiredMode = Schema.RequiredMode.REQUIRED)
    private int userId;

    @Schema(description = "ID produk", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private int productId;

    @Schema(description = "Nama pengguna", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Min(value = 1, message = "Rating harus antara 1 sampai 5")
    @Max(value = 5, message = "Rating harus antara 1 sampai 5")
    @Schema(description = "Rating (1-5)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    private int rating;

    @NotBlank(message = "Komentar wajib diisi")
    @Schema(description = "Komentar review", example = "Produk sangat bagus!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String comment;
}
