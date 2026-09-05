package com.sanedge.slider.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.slider.entity.Slider;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SliderQueryRepository implements PanacheRepository<Slider> {

    public Uni<PagedResult<Slider>> findSliders(com.sanedge.slider.domain.requests.FindAllSliderRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        io.quarkus.hibernate.reactive.panache.PanacheQuery<Slider> panacheQuery;
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

    public Uni<PagedResult<Slider>> findActiveSliders(com.sanedge.slider.domain.requests.FindAllSliderRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        io.quarkus.hibernate.reactive.panache.PanacheQuery<Slider> panacheQuery;
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

    public Uni<PagedResult<Slider>> findTrashedSliders(com.sanedge.slider.domain.requests.FindAllSliderRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        io.quarkus.hibernate.reactive.panache.PanacheQuery<Slider> panacheQuery;
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
}
