package com.sanedge.review_detail.domain.requests;

import org.jboss.resteasy.reactive.RestForm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat detail review (contoh: gambar/video)")
public class CreateReviewDetailRequest {

    @NotNull(message = "ID review wajib diisi")
    @RestForm
    @Schema(description = "ID review yang terkait", example = "202", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reviewId;

    @NotBlank(message = "Tipe media wajib diisi")
    @RestForm
    @Schema(description = "Tipe media", allowableValues = { "image",
            "video" }, example = "image", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotNull(message = "File wajib diunggah")
    @RestForm
    @Schema(description = "File media (gambar atau video)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String file;

    @NotBlank(message = "Caption wajib diisi")
    @RestForm
    @Schema(description = "Keterangan media", example = "Foto produk", requiredMode = Schema.RequiredMode.REQUIRED)
    private String caption;
}