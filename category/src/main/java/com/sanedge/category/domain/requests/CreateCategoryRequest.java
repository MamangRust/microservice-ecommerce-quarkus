package com.sanedge.category.domain.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCategoryRequest {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotBlank(message = "Slug cannot be blank")
    private String slugCategory;

    @NotBlank(message = "Image cannot be blank")
    private String imageCategory;
}
