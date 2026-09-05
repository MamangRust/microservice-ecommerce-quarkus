package com.sanedge.slider.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
@Schema(description = "Request untuk mengambil semua slider dengan paging dan filter search")
public class FindAllSliderRequest {

    @NotNull
    @QueryParam("search")
    @DefaultValue("")
    @Parameter(description = "Keyword pencarian slider", example = "promo")
    private String search = "";

    @Min(1)
    @QueryParam("page")
    @DefaultValue("1")
    @Parameter(description = "Nomor halaman", example = "1")
    private int page = 1;

    @Min(1)
    @QueryParam("pageSize")
    @DefaultValue("10")
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private int pageSize = 10;
}