package com.sanedge.category.domain.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCategoryRequest {

    @NotNull(message = "Category ID cannot be null")
    private Integer categoryId;

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotBlank(message = "Slug cannot be blank")
    private String slugCategory;

    private String imageCategory;
}
