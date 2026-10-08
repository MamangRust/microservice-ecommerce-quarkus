package com.sanedge.common.adapter.order;

import com.sanedge.common.adapter.model.Order;
import com.sanedge.common.adapter.support.Paged;

import io.smallrye.mutiny.Uni;

/**
 * Port order. Setara {@code order.QueryRepository} + {@code order.BulkRepository}
 * di Go. Error status gRPC dipropagasikan apa adanya (NotFound -&gt; 404).
 */
public interface OrderPort {

    Uni<Order> findById(int orderId);

    Uni<Paged<Order>> findAll(int page, int pageSize);
}
