package com.sanedge.slider.domain.requests;

import org.jboss.resteasy.reactive.RestForm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat slider baru")
public class CreateSliderRequest {

    @NotNull
    @RestForm
    @Schema(description = "Nama slider", example = "Promo Akhir Tahun")
    private String nama;

    @NotNull
    @RestForm
    @Schema(description = "Gambar slider")
    private String filePath;
}