package com.sanedge.banner.domain.response;

import com.sanedge.banner.entity.Banner;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private Long id;
    private String name;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private Boolean isActive;
    private String createdAt;
    private String updatedAt;

    public static BannerResponse from(Banner banner) {
        return BannerResponse.builder()
                .id(banner.id)
                .name(banner.getName())
                .startDate(banner.getStartDate() != null ? banner.getStartDate().toString() : null)
                .endDate(banner.getEndDate() != null ? banner.getEndDate().toString() : null)
                .startTime(banner.getStartTime() != null ? banner.getStartTime().toString() : null)
                .endTime(banner.getEndTime() != null ? banner.getEndTime().toString() : null)
                .isActive(banner.getIsActive())
                .createdAt(banner.getCreatedAt() != null ? banner.getCreatedAt().toString() : null)
                .updatedAt(banner.getUpdatedAt() != null ? banner.getUpdatedAt().toString() : null)
                .build();
    }
}
