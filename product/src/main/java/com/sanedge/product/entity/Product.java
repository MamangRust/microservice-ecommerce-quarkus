package com.sanedge.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "products")
public class Product extends BaseModel {

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer price;

    @Column(name = "count_in_stock", nullable = false)
    private Integer countInStock;

    private String brand;

    private Integer weight;

    private Float rating;

    @Column(name = "slug_product", unique = true, length = 100)
    private String slugProduct;

    @Column(name = "image_product", columnDefinition = "TEXT")
    private String imageProduct;
}
