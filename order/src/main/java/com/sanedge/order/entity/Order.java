package com.sanedge.order.entity;

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
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "orders")
public class Order extends BaseModel {

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;
}
