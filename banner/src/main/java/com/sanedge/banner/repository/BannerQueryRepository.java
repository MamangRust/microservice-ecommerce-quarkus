package com.sanedge.banner.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.banner.entity.Banner;
import com.sanedge.banner.domain.requests.FindAllBannerRequest;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BannerQueryRepository implements PanacheRepository<Banner> {

    public Uni<PagedResult<Banner>> findBanners(FindAllBannerRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        io.quarkus.hibernate.reactive.panache.PanacheQuery<Banner> panacheQuery;
        if (keyword.isEmpty()) {
            panacheQuery = find("deletedAt IS NULL").page(page, size);
        } else {
            var query = """
                        deletedAt IS NULL
                        AND LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                    """;
            panacheQuery = find(query, keyword).page(page, size);
        }

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Banner>> findActiveBanners(FindAllBannerRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        io.quarkus.hibernate.reactive.panache.PanacheQuery<Banner> panacheQuery;
        if (keyword.isEmpty()) {
            panacheQuery = find("deletedAt IS NULL AND isActive = true").page(page, size);
        } else {
            var query = """
                        deletedAt IS NULL
                        AND isActive = true
                        AND LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                    """;
            panacheQuery = find(query, keyword).page(page, size);
        }

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Banner>> findTrashedBanners(FindAllBannerRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        io.quarkus.hibernate.reactive.panache.PanacheQuery<Banner> panacheQuery;
        if (keyword.isEmpty()) {
            panacheQuery = find("deletedAt IS NOT NULL").page(page, size);
        } else {
            var query = """
                        deletedAt IS NOT NULL
                        AND LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                    """;
            panacheQuery = find(query, keyword).page(page, size);
        }

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Banner> findByName(String name) {
        return find("LOWER(name) = LOWER(?1) AND deletedAt IS NULL", name).firstResult();
    }
}
