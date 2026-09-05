package com.sanedge.category.domain.requests;

import lombok.Data;

@Data
public class FindAllCategoryRequest {
    private String search = "";
    private Integer page = 1;
    private Integer pageSize = 10;
}
