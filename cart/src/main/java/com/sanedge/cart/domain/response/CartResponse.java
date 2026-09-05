package com.sanedge.cart.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import com.sanedge.cart.entity.Cart;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long id;
    private Integer userId;
    private Integer productId;
    private String name;
    private Integer price;
    private String image;
    private Integer quantity;
    private Integer weight;
    private String createdAt;
    private String updatedAt;

    public static CartResponse from(Cart cart) {
        return CartResponse.builder()
                .id(cart.id)
                .userId(cart.getUserId())
                .productId(cart.getProductId())
                .name(cart.getName())
                .price(cart.getPrice())
                .image(cart.getImage())
                .quantity(cart.getQuantity())
                .weight(cart.getWeight())
                .createdAt(cart.getCreatedAt() != null ? cart.getCreatedAt().toString() : null)
                .updatedAt(cart.getUpdatedAt() != null ? cart.getUpdatedAt().toString() : null)
                .build();
    }
}