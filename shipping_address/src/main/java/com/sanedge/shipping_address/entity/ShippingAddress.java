package com.sanedge.shipping_address.entity;

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
@Table(name = "shipping_addresses")
public class ShippingAddress extends BaseModel {
    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String alamat;

    @Column(nullable = false, length = 255)
    private String provinsi;

    @Column(nullable = false, length = 255)
    private String negara;

    @Column(nullable = false, length = 255)
    private String kota;

    @Column(nullable = false, length = 100)
    private String courier;

    @Column(name = "shipping_method", nullable = false, length = 255)
    private String shippingMethod;

    @Column(name = "shipping_cost", nullable = false)
    private Integer shippingCost;
}
