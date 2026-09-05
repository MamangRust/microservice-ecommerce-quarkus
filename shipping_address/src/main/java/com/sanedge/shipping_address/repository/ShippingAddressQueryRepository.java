package com.sanedge.shipping_address.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.shipping_address.entity.ShippingAddress;
import com.sanedge.shipping_address.domain.requests.FindAllShippingAddress;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class ShippingAddressQueryRepository implements PanacheRepository<ShippingAddress> {

    public Uni<PagedResult<ShippingAddress>> findShippingAddresses(FindAllShippingAddress req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NULL
                    AND (
LOWER(kota) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(provinsi) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(negara) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<ShippingAddress>> findActiveShippingAddresses(FindAllShippingAddress req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NULL
                    AND (
LOWER(kota) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(provinsi) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(negara) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<ShippingAddress>> findTrashedShippingAddresses(FindAllShippingAddress req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NOT NULL
                    AND (
LOWER(kota) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(provinsi) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(negara) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Optional<ShippingAddress>> findByIdNative(Long id) {
        return find("id = ?1 AND deletedAt IS NULL", id).firstResult().map(Optional::ofNullable);
    }

    public Uni<Optional<ShippingAddress>> findByOrderId(Integer orderId) {
        return find("orderId = ?1 AND deletedAt IS NULL", orderId).firstResult().map(Optional::ofNullable);
    }
}
