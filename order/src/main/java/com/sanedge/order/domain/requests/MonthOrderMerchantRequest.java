package com.sanedge.order.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Data
@Schema(description = "Request untuk mengambil statistik order bulanan dari merchant tertentu")
public class MonthOrderMerchantRequest {

    @NotNull(message = "ID merchant wajib diisi")
    @QueryParam("merchantId")
    @Schema(description = "ID merchant", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer merchantId;

    @NotNull(message = "Tahun wajib diisi")
    @QueryParam("year")
    @Schema(description = "Tahun (misalnya: 2024)", example = "2024", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer year;

    @NotNull(message = "Bulan wajib diisi")
    @Min(value = 1, message = "Bulan harus antara 1–12")
    @Max(value = 12, message = "Bulan harus antara 1–12")
    @QueryParam("month")
    @Schema(description = "Bulan (1–12)", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer month;
}
