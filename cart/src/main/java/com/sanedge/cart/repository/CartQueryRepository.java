package com.sanedge.cart.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.cart.entity.Cart;
import com.sanedge.cart.domain.requests.FindAllCartsRequest;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CartQueryRepository implements PanacheRepository<Cart> {

    public Uni<PagedResult<Cart>> findCartsByUser(FindAllCartsRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
        Integer userId = req.getUserId();

        var query = """
                    deletedAt IS NULL
                    AND userId = ?1
                    AND (
                        LOWER(name) LIKE LOWER(CONCAT('%', ?2, '%'))
                        OR CAST(price AS string) LIKE CONCAT('%', ?2, '%')
                    )
                """;

        var panacheQuery = find(query, userId, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }
}
