package com.sanedge.order.domain.requests;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
@Schema(description = "Request untuk mencari semua order dengan pagination dan pencarian")
public class FindAllOrderRequest {

    @QueryParam("search")
    @DefaultValue("")
    @Parameter(description = "Kata kunci pencarian", example = "produk")
    private String search = "";

    @Min(1)
    @QueryParam("page")
    @DefaultValue("1")
    @Parameter(description = "Nomor halaman (mulai dari 1)", example = "1")
    private int page = 1;

    @Min(1)
    @QueryParam("pageSize")
    @DefaultValue("10")
    @Parameter(description = "Jumlah data per halaman", example = "10")
    private int pageSize = 10;
}
