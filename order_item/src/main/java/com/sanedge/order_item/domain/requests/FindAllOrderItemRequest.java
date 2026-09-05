package com.sanedge.order_item.domain.requests;

import lombok.Data;

@Data
public class FindAllOrderItemRequest {
    private Integer page = 1;
    private Integer pageSize = 10;
    private String search = "";
}
