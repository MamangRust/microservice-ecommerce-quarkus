package com.sanedge.common.adapter.shipping_address;

import com.sanedge.common.adapter.model.ShippingAddress;

import io.smallrye.mutiny.Uni;

/**
 * Port shipping address. Setara
 * {@code shipping_address.QueryRepository}/{@code CommandRepository} di Go.
 * Read path mempropagasikan status gRPC apa adanya.
 */
public interface ShippingAddressPort {

    Uni<ShippingAddress> findById(int shippingId);

    Uni<ShippingAddress> findByOrder(int orderId);

    Uni<ShippingAddress> create(CreateData data);

    Uni<ShippingAddress> update(UpdateData data);

    Uni<Boolean> deleteByOrderIdPermanent(int orderId);

    Uni<Boolean> deleteAll();

    record CreateData(
            int orderId,
            String alamat,
            String provinsi,
            String kota,
            String negara,
            String courier,
            String shippingMethod,
            int shippingCost) {
    }

    record UpdateData(
            int shippingId,
            String alamat,
            String provinsi,
            String kota,
            String negara,
            String courier,
            String shippingMethod,
            int shippingCost) {
    }
}
