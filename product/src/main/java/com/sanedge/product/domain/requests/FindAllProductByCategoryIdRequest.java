package com.sanedge.product.domain.requests;

import lombok.Data;

@Data
public class FindAllProductByCategoryIdRequest {
    private Integer categoryId;
    private String search = "";
    private Integer page = 1;
    private Integer pageSize = 10;
}
