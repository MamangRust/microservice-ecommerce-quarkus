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
public class ProductResponseDeleteAt {
    private Long id;
    private Integer merchantId;
    private Integer categoryId;
    private String name;
    private String description;
    private String price;
    private String countInStock;
    private String brand;
    private String weight;
    private String rating;
    private String slugProduct;
    private String imageProduct;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static ProductResponseDeleteAt from(Product entity) {
        return ProductResponseDeleteAt.builder()
                .id(entity.id)
                .merchantId(entity.getMerchantId())
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice() != null ? entity.getPrice().toString() : null)
                .countInStock(entity.getCountInStock() != null ? entity.getCountInStock().toString() : null)
                .brand(entity.getBrand())
                .weight(entity.getWeight() != null ? entity.getWeight().toString() : null)
                .rating(entity.getRating() != null ? entity.getRating().toString() : null)
                .slugProduct(entity.getSlugProduct())
                .imageProduct(entity.getImageProduct())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .deletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toString() : null)
                .build();
    }
}