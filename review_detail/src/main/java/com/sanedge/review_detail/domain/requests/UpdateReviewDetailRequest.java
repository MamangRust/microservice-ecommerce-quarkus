package com.sanedge.review_detail.domain.requests;

import org.jboss.resteasy.reactive.RestForm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk memperbarui detail review yang sudah ada")
public class UpdateReviewDetailRequest {

    @NotNull(message = "ID detail review wajib diisi")
    @RestForm
    @Schema(description = "ID detail review", example = "303", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer reviewDetailId;

    @NotBlank(message = "Tipe media wajib diisi")
    @RestForm
    @Schema(description = "Tipe media", allowableValues = { "image",
            "video" }, example = "video", requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    @NotNull(message = "File wajib diunggah")
    @RestForm
    @Schema(description = "File media (gambar atau video)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String file;

    @NotBlank(message = "Caption wajib diisi")
    @RestForm
    @Schema(description = "Keterangan media", example = "Video unboxing", requiredMode = Schema.RequiredMode.REQUIRED)
    private String caption;
}