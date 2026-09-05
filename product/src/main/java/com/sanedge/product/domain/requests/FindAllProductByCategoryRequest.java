package com.sanedge.product.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
@Schema(name = "FindAllProductByCategoryRequest", description = "Request untuk mengambil produk berdasarkan category name dengan filter")
public class FindAllProductByCategoryRequest {

    @NotNull
    @QueryParam("categoryName")
    @Schema(description = "Nama kategori produk", example = "Minuman", requiredMode = Schema.RequiredMode.REQUIRED)
    private String categoryName;

    @QueryParam("search")
    @DefaultValue("")
    @Parameter(description = "Kata kunci pencarian (nullable)", example = "kopi")
    private String search = "";

    @Min(0)
    @QueryParam("minPrice")
    @DefaultValue("0")
    @Parameter(description = "Harga minimum (nullable)", example = "0")
    private Integer minPrice = 0;

    @Min(0)
    @QueryParam("maxPrice")
    @DefaultValue("999999999")
    @Parameter(description = "Harga maksimum (nullable)", example = "100000")
    private Integer maxPrice = 999999999;

    @NotNull
    @Min(1)
    @QueryParam("page")
    @DefaultValue("1")
    @Parameter(description = "Nomor halaman (mulai dari 1)", example = "1", required = true)
    private Integer page = 1;

    @NotNull
    @Min(1)
    @QueryParam("pageSize")
    @DefaultValue("10")
    @Parameter(description = "Jumlah data per halaman", example = "10", required = true)
    private Integer pageSize = 10;
}