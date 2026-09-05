package com.sanedge.review.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.review.entity.Review;
import com.sanedge.review.entity.ReviewDetail;
import com.sanedge.review.entity.ReviewRelationsDetail;
import com.sanedge.review.domain.requests.FindAllReview;
import com.sanedge.review.domain.requests.FindAllReviewByMerchant;
import com.sanedge.review.domain.requests.FindAllReviewByProduct;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReviewQueryRepository implements PanacheRepository<Review> {

    private final ObjectMapper mapper = new ObjectMapper();

    public Uni<PagedResult<Review>> findReviews(FindAllReview req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NULL
                    AND (
LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(comment) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;
        var panacheQuery = find(query, keyword).page(page, size);
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Review>> findActiveReviews(FindAllReview req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NULL
                    AND (
LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(comment) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;
        var panacheQuery = find(query, keyword).page(page, size);
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Review>> findTrashedReviews(FindAllReview req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NOT NULL
                    AND (
LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(comment) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;
        var panacheQuery = find(query, keyword).page(page, size);
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<Optional<Review>> findReviewById(Long reviewId) {
        return find("id = ?1 AND deletedAt IS NULL", reviewId).firstResult().map(Optional::ofNullable);
    }

    public Uni<PagedResult<ReviewRelationsDetail>> findByMerchantId(FindAllReviewByMerchant req) {
        Integer merchantId = req.getMerchantId();
        Integer rating = req.getRating() == null ? 0 : req.getRating();
        String search = req.getSearch() == null ? "" : req.getSearch();
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;

        String baseSql = """
                FROM reviews r
                JOIN products p ON r.product_id = p.id
                WHERE r.deleted_at IS NULL
                  AND p.merchant_id = :merchantId
                  AND (:rating = 0 OR r.rating = :rating)
                  AND (
                        :search = ''
                        OR r.name ILIKE CONCAT('%', :search, '%')
                        OR r.comment ILIKE CONCAT('%', :search, '%')
                      )
                """;

        String countSql = "SELECT COUNT(*) " + baseSql;
        String dataSql = """
                SELECT
                    r.id,
                    r.user_id,
                    r.product_id,
                    r.name,
                    r.comment,
                    r.rating,
                    r.created_at,
                    r.updated_at,
                    r.deleted_at,
                    COALESCE(
                        (SELECT json_agg(
                            jsonb_build_object(
                                'detail_id', rd.id,
                                'type', rd.type,
                                'url', rd.url,
                                'caption', rd.caption,
                                'created_at', rd.created_at
                            )
                        )
                        FROM review_details rd
                        WHERE rd.review_id = r.id),
                        '[]'
                    ) AS review_details
                """ + baseSql + """
                ORDER BY r.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        return Panache.getSession().chain(session -> {
            var countQuery = session.createNativeQuery(countSql);
            countQuery.setParameter("merchantId", merchantId);
            countQuery.setParameter("rating", rating);
            countQuery.setParameter("search", search == null || search.isBlank() ? "" : search);

            return countQuery.getSingleResult().chain(countVal -> {
                long total = ((Number) countVal).longValue();

                var dataQuery = session.createNativeQuery(dataSql);
                dataQuery.setParameter("merchantId", merchantId);
                dataQuery.setParameter("rating", rating);
                dataQuery.setParameter("search", search == null || search.isBlank() ? "" : search);
                dataQuery.setParameter("limit", size);
                dataQuery.setParameter("offset", page * size);

                return dataQuery.getResultList().map(results -> {
                    List<ReviewRelationsDetail> relations = mapResults(results);
                    return new PagedResult<>(relations, (int) total);
                });
            });
        });
    }

    public Uni<PagedResult<ReviewRelationsDetail>> findByProductId(FindAllReviewByProduct req) {
        Integer productId = req.getProductId();
        Integer rating = req.getRating() == null ? 0 : req.getRating();
        String search = req.getSearch() == null ? "" : req.getSearch();
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;

        String baseSql = """
                FROM reviews r
                WHERE r.deleted_at IS NULL
                  AND r.product_id = :productId
                  AND (:rating = 0 OR r.rating = :rating)
                  AND (
                        :search = ''
                        OR r.name ILIKE CONCAT('%', :search, '%')
                        OR r.comment ILIKE CONCAT('%', :search, '%')
                      )
                """;

        String countSql = "SELECT COUNT(*) " + baseSql;
        String dataSql = """
                SELECT
                    r.id,
                    r.user_id,
                    r.product_id,
                    r.name,
                    r.comment,
                    r.rating,
                    r.created_at,
                    r.updated_at,
                    r.deleted_at,
                    COALESCE(
                        (SELECT json_agg(
                            jsonb_build_object(
                                'detail_id', rd.id,
                                'type', rd.type,
                                'url', rd.url,
                                'caption', rd.caption,
                                'created_at', rd.created_at
                            )
                        )
                        FROM review_details rd
                        WHERE rd.review_id = r.id),
                        '[]'
                    ) AS review_details
                """ + baseSql + """
                ORDER BY r.created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        return Panache.getSession().chain(session -> {
            var countQuery = session.createNativeQuery(countSql);
            countQuery.setParameter("productId", productId);
            countQuery.setParameter("rating", rating);
            countQuery.setParameter("search", search == null || search.isBlank() ? "" : search);

            return countQuery.getSingleResult().chain(countVal -> {
                long total = ((Number) countVal).longValue();

                var dataQuery = session.createNativeQuery(dataSql);
                dataQuery.setParameter("productId", productId);
                dataQuery.setParameter("rating", rating);
                dataQuery.setParameter("search", search == null || search.isBlank() ? "" : search);
                dataQuery.setParameter("limit", size);
                dataQuery.setParameter("offset", page * size);

                return dataQuery.getResultList().map(results -> {
                    List<ReviewRelationsDetail> relations = mapResults(results);
                    return new PagedResult<>(relations, (int) total);
                });
            });
        });
    }

    private List<ReviewRelationsDetail> mapResults(List<?> results) {
        List<ReviewRelationsDetail> list = new ArrayList<>();
        for (Object item : results) {
            Object[] row = (Object[]) item;
            ReviewRelationsDetail dto = new ReviewRelationsDetail();
            dto.setId(((Number) row[0]).intValue());
            dto.setUserId(((Number) row[1]).intValue());
            dto.setProductId(((Number) row[2]).intValue());
            dto.setName((String) row[3]);
            dto.setComment((String) row[4]);
            dto.setRating(((Number) row[5]).intValue());
            dto.setCreatedAt(row[6] != null ? row[6].toString() : null);
            dto.setUpdatedAt(row[7] != null ? row[7].toString() : null);
            dto.setDeletedAt(row[8] != null ? row[8].toString() : null);

            if (row[9] != null) {
                try {
                    ReviewDetail[] detailsArray = mapper.readValue(row[9].toString(), ReviewDetail[].class);
                    dto.setReviewDetail(Arrays.asList(detailsArray));
                } catch (Exception e) {
                    dto.setReviewDetail(Collections.emptyList());
                }
            } else {
                dto.setReviewDetail(Collections.emptyList());
            }
            list.add(dto);
        }
        return list;
    }
}
