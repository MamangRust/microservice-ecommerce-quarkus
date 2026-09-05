package com.sanedge.banner.domain.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBannerRequest {

    @NotBlank(message = "Name wajib di isi")
    private String name;

    @NotBlank(message = "Start date wajib di isi")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Format tanggal mulai YYYY-MM-DD")
    private String startDate;

    @NotBlank(message = "End date wajib di isi")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Format tanggal akhir YYYY-MM-DD")
    private String endDate;

    @NotBlank(message = "Start time wajib di isi")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "Format waktu mulai HH:mm")
    private String startTime;

    @NotBlank(message = "End time wajib di isi")
    @Pattern(regexp = "\\d{2}:\\d{2}", message = "Format waktu akhir HH:mm")
    private String endTime;

    @NotNull(message = "Is active wajib di isi")
    private Boolean isActive;
}
