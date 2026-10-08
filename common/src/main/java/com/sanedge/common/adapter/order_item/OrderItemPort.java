package com.sanedge.common.adapter.order_item;

import java.util.List;

import com.sanedge.common.adapter.model.OrderItem;
import com.sanedge.common.adapter.support.Paged;

import io.smallrye.mutiny.Uni;

/**
 * Port order item: baca + tulis + enumerasi. Setara
 * {@code order_item.QueryRepository}/{@code CommandRepository}/{@code BulkRepository}
 * di Go.
 */
public interface OrderItemPort {

    Uni<List<OrderItem>> findOrderItemByOrder(int orderId);

    Uni<Integer> calculateTotalPrice(int orderId);

    Uni<OrderItem> create(CreateData data);

    Uni<OrderItem> update(UpdateData data);

    Uni<OrderItem> trash(int orderItemId);

    Uni<OrderItem> restore(int orderItemId);

    Uni<Boolean> deletePermanent(int orderItemId);

    Uni<Boolean> deleteByOrderIdPermanent(int orderId);

    /** Rollback kompensasi: hapus item milik order (dipakai saat create order gagal). */
    Uni<Boolean> deleteByOrderIdRollback(int orderId);

    Uni<Boolean> restoreAll();

    Uni<Boolean> deleteAll();

    Uni<Paged<OrderItem>> findAll(int page, int pageSize);

    record CreateData(int orderId, int productId, int quantity, int price) {
    }

    record UpdateData(int orderItemId, int quantity, int price) {
    }
}
