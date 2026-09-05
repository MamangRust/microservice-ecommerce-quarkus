package com.sanedge.category.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.category.entity.Category;
import com.sanedge.category.domain.requests.FindAllCategoryRequest;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CategoryQueryRepository implements PanacheRepository<Category> {

    public Uni<PagedResult<Category>> findCategories(FindAllCategoryRequest req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Category> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NULL", Sort.ascending("id")).page(page, size);
        } else {
            var query = "deletedAt IS NULL AND ("
                    + "LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))"
                    + " OR LOWER(description) LIKE LOWER(CONCAT('%', ?1, '%'))"
                    + " OR LOWER(slugCategory) LIKE LOWER(CONCAT('%', ?1, '%')))";
            panacheQuery = find(query, Sort.ascending("id"), keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Category>> findActiveCategories(FindAllCategoryRequest req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Category> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NULL", Sort.ascending("id")).page(page, size);
        } else {
            var query = "deletedAt IS NULL AND ("
                    + "LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))"
                    + " OR LOWER(description) LIKE LOWER(CONCAT('%', ?1, '%'))"
                    + " OR LOWER(slugCategory) LIKE LOWER(CONCAT('%', ?1, '%')))";
            panacheQuery = find(query, Sort.ascending("id"), keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Category>> findTrashedCategories(FindAllCategoryRequest req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Category> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NOT NULL", Sort.descending("id")).page(page, size);
        } else {
            var query = "deletedAt IS NOT NULL AND ("
                    + "LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))"
                    + " OR LOWER(description) LIKE LOWER(CONCAT('%', ?1, '%'))"
                    + " OR LOWER(slugCategory) LIKE LOWER(CONCAT('%', ?1, '%')))";
            panacheQuery = find(query, Sort.descending("id"), keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<Category> findCategoryById(Long categoryId) {
        return find("id = ?1 AND deletedAt IS NULL", categoryId).firstResult();
    }
}
