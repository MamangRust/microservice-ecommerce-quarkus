package com.sanedge.order_item.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.order_item.domain.requests.FindAllOrderItemRequest;
import com.sanedge.order_item.entity.OrderItem;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderItemRepository implements PanacheRepository<OrderItem> {

    public Uni<List<OrderItem>> findOrderItemByOrder(Long orderId) {
        return list("orderId = ?1 AND deletedAt IS NULL", orderId.intValue());
    }

    public Uni<PagedResult<OrderItem>> findOrderItems(FindAllOrderItemRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        
        var panacheQuery = find("deletedAt IS NULL").page(page, size);
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<OrderItem>> findActiveOrderItems(FindAllOrderItemRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        
        var panacheQuery = find("deletedAt IS NULL").page(page, size);
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<OrderItem>> findTrashedOrderItems(FindAllOrderItemRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        
        var panacheQuery = find("deletedAt IS NOT NULL").page(page, size);
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    @WithTransaction
    public Uni<OrderItem> trash(Long orderItemId) {
        return findById(orderItemId)
                .chain(item -> {
                    if (item != null && item.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        item.setDeletedAt(Timestamp.valueOf(date));
                        return persist(item).map(v -> item);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<OrderItem> restore(Long orderItemId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", orderItemId).firstResult()
                .chain(item -> {
                    if (item != null) {
                        item.setDeletedAt(null);
                        return persist(item).map(v -> item);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<OrderItem> deletePermanent(Long orderItemId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", orderItemId).firstResult()
                .chain(item -> {
                    if (item != null) {
                        return delete(item).map(v -> item);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteByOrderPermanent(Long orderId) {
        return delete("orderId = ?1 AND deletedAt IS NOT NULL", orderId.intValue())
                .map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteByOrderRollback(Long orderId) {
        return delete("orderId = ?1", orderId.intValue())
                .map(count -> count > 0);
    }
}
