package com.sanedge.product.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
@Schema(name = "FindAllProductByMerchantRequest", description = "Request untuk mengambil produk by merchant dengan filter")
public class FindAllProductByMerchantRequest {

    @NotNull
    @QueryParam("merchantId")
    @Schema(description = "ID merchant", example = "1")
    private Integer merchantId;

    @QueryParam("search")
    @DefaultValue("")
    @Parameter(description = "Kata kunci pencarian (name/description/slug)", example = "kopi")
    private String search = "";

    @NotNull
    @QueryParam("categoryId")
    @DefaultValue("0")
    @Parameter(description = "ID kategori (0/null untuk semua)", example = "0")
    private Integer categoryId = 0;

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
    @Parameter(description = "Nomor halaman (mulai dari 1)", example = "1")
    private Integer page = 1;

    @NotNull
    @Min(1)
    @QueryParam("pageSize")
    @DefaultValue("10")
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private Integer pageSize = 10;
}
