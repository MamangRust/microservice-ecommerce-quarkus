package com.sanedge.slider.domain.response;

import com.sanedge.slider.entity.Slider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderResponse {
    private Long id;
    private String name;
    private String image;
    private String createdAt;
    private String updatedAt;

    public static SliderResponse from(Slider entity) {
        return SliderResponse.builder()
                .id(entity.id)
                .name(entity.getName())
                .image(entity.getImage())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }
}