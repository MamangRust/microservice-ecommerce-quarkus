package com.sanedge.order.repository;

import java.util.Optional;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.order.entity.Order;

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderQueryRepository implements PanacheRepository<Order> {

    public Uni<PagedResult<Order>> findOrders(com.sanedge.order.domain.requests.FindAllOrderRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Order> panacheQuery;
        if (keyword == null) {
            panacheQuery = findAll().page(page, size);
        } else {
            var query = "(CAST(id AS string) LIKE CONCAT('%', ?1, '%')"
                    + " OR CAST(totalPrice AS string) LIKE CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Order>> findActiveOrders(com.sanedge.order.domain.requests.FindAllOrderRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Order> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NULL").page(page, size);
        } else {
            var query = "deletedAt IS NULL AND ("
                    + "CAST(id AS string) LIKE CONCAT('%', ?1, '%')"
                    + " OR CAST(totalPrice AS string) LIKE CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Order>> findTrashedOrders(com.sanedge.order.domain.requests.FindAllOrderRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Order> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NOT NULL").page(page, size);
        } else {
            var query = "deletedAt IS NOT NULL AND ("
                    + "CAST(id AS string) LIKE CONCAT('%', ?1, '%')"
                    + " OR CAST(totalPrice AS string) LIKE CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Order>> findOrdersByMerchant(
            com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
        Integer merchantId = req.getMerchantId();

        PanacheQuery<Order> panacheQuery;
        if (keyword == null && merchantId == null) {
            panacheQuery = find("deletedAt IS NULL").page(page, size);
        } else if (keyword == null) {
            panacheQuery = find("deletedAt IS NULL AND merchantId = ?1", merchantId).page(page, size);
        } else if (merchantId == null) {
            var query = "deletedAt IS NULL AND ("
                    + "CAST(id AS string) LIKE CONCAT('%', ?1, '%')"
                    + " OR CAST(totalPrice AS string) LIKE CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        } else {
            var query = "deletedAt IS NULL AND merchantId = ?2 AND ("
                    + "CAST(id AS string) LIKE CONCAT('%', ?1, '%')"
                    + " OR CAST(totalPrice AS string) LIKE CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword, merchantId).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<Optional<Order>> findOrderById(Long orderId) {
        return find("id = ?1 AND deletedAt IS NULL", orderId).firstResult().map(Optional::ofNullable);
    }
}
