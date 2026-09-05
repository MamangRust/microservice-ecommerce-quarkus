package com.sanedge.slider.domain.requests;

import org.jboss.resteasy.reactive.RestForm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk memperbarui slider")
public class UpdateSliderRequest {

    @NotNull
    @RestForm
    @Schema(description = "ID slider yang akan diperbarui", example = "1")
    private Integer id;

    @NotNull
    @RestForm
    @Schema(description = "Nama slider", example = "Promo Akhir Tahun")
    private String nama;

    @RestForm
    @Schema(description = "Gambar slider baru (opsional)")
    private String filePath;
}