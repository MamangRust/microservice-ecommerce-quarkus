package com.sanedge.product.domain.response;

import com.sanedge.product.entity.Product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private Integer merchantId;
    private Integer categoryId;
    private String name;
    private String description;
    private Integer price;
    private Integer countInStock;
    private String brand;
    private Integer weight;
    private Float rating;
    private String slugProduct;
    private String imageProduct;
    private String createdAt;
    private String updatedAt;

    public static ProductResponse from(Product entity) {
        return ProductResponse.builder()
                .id(entity.id)
                .merchantId(entity.getMerchantId())
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .countInStock(entity.getCountInStock())
                .brand(entity.getBrand())
                .weight(entity.getWeight())
                .rating(entity.getRating())
                .slugProduct(entity.getSlugProduct())
                .imageProduct(entity.getImageProduct())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }
}