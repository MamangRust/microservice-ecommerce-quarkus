package com.sanedge.common.adapter.model;

import java.time.Instant;

/** Domain model shipping address (setara {@code models.ShippingAddress} di Go). */
public record ShippingAddress(
        int id,
        int orderId,
        String alamat,
        String provinsi,
        String negara,
        String kota,
        String shippingMethod,
        int shippingCost,
        Instant createdAt,
        Instant updatedAt) {
}
