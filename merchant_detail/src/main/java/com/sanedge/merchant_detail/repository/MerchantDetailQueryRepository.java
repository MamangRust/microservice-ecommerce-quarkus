package com.sanedge.merchant_detail.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant_detail.entity.MerchantDetail;
import com.sanedge.merchant_detail.entity.MerchantDetailsRelation;
import com.sanedge.merchant_detail.entity.MerchantSocialMediaLink;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantDetailQueryRepository
        implements PanacheRepository<MerchantDetail>, MerchantDetailQueryRepositoryCustom {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Uni<PagedResult<MerchantDetailsRelation>> findAllWithSocialLinks(com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
        return queryWithSocialLinks(search, null, null, page, size);
    }

    @Override
    public Uni<PagedResult<MerchantDetailsRelation>> findActiveWithSocialLinks(com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
        return queryWithSocialLinks(search, true, null, page, size);
    }

    @Override
    public Uni<PagedResult<MerchantDetailsRelation>> findTrashedWithSocialLinks(com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
        return queryWithSocialLinks(search, false, null, page, size);
    }

    @Override
    public Uni<Optional<MerchantDetailsRelation>> findByIdWithSocialLinks(Long merchantDetailId) {
        return queryWithSocialLinks(null, null, merchantDetailId, 0, 1)
                .map(paged -> {
                    if (paged.getData().isEmpty()) {
                        return Optional.empty();
                    }
                    return Optional.of(paged.getData().get(0));
                });
    }

    private Uni<PagedResult<MerchantDetailsRelation>> queryWithSocialLinks(String keyword, Boolean isActive, Long id,
            int page, int size) {
        StringBuilder baseSql = new StringBuilder("FROM merchant_details md " +
                "JOIN merchants m ON md.merchant_id = m.id " +
                "LEFT JOIN merchant_social_media_links sml ON sml.merchant_detail_id = md.id " +
                "WHERE 1=1 ");

        if (keyword != null) {
            baseSql.append("AND LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ");
        }
        if (isActive != null) {
            baseSql.append(isActive ? "AND m.deleted_at IS NULL " : "AND m.deleted_at IS NOT NULL ");
        }
        if (id != null) {
            baseSql.append("AND md.id = :id ");
        }

        String countSql = "SELECT COUNT(DISTINCT md.id) " + baseSql.toString();

        String dataSql = "SELECT " +
                "md.id, md.merchant_id, md.display_name, md.cover_image_url, md.logo_url, " +
                "md.short_description, md.website_url, md.created_at, md.updated_at, md.deleted_at, " +
                "m.name AS merchant_name, " +
                "json_agg(json_build_object('id', sml.id, 'platform', sml.platform, 'url', sml.url)) AS social_media_links "
                + baseSql.toString() +
                "GROUP BY md.id, m.id " +
                "ORDER BY md.created_at DESC " +
                "LIMIT :limit OFFSET :offset";

        return Panache.getSession().chain(session -> {
            var countQuery = session.createNativeQuery(countSql);
            if (keyword != null)
                countQuery.setParameter("keyword", keyword);
            if (id != null)
                countQuery.setParameter("id", id);

            return countQuery.getSingleResult().chain(countVal -> {
                long total = ((Number) countVal).longValue();

                var dataQuery = session.createNativeQuery(dataSql);
                if (keyword != null)
                    dataQuery.setParameter("keyword", keyword);
                if (id != null)
                    dataQuery.setParameter("id", id);
                dataQuery.setParameter("limit", size);
                dataQuery.setParameter("offset", page * size);

                return dataQuery.getResultList().map(results -> {
                    List<MerchantDetailsRelation> relations = mapResults(results);
                    return new PagedResult<>(relations, (int) total);
                });
            });
        });
    }

    private List<MerchantDetailsRelation> mapResults(List<?> results) {
        List<MerchantDetailsRelation> relations = new ArrayList<>();
        for (Object item : results) {
            Object[] row = (Object[]) item;
            MerchantDetailsRelation dto = new MerchantDetailsRelation();
            dto.setId(((Number) row[0]).intValue());
            dto.setMerchantId(((Number) row[1]).intValue());
            dto.setDisplayName((String) row[2]);
            dto.setCoverImageUrl((String) row[3]);
            dto.setLogoUrl((String) row[4]);
            dto.setShortDescription((String) row[5]);
            dto.setWebsiteUrl((String) row[6]);
            dto.setCreatedAt(row[7] != null ? row[7].toString() : null);
            dto.setUpdatedAt(row[8] != null ? row[8].toString() : null);
            dto.setDeletedAt(row[9] != null ? row[9].toString() : null);

            if (row[11] != null) {
                try {
                    List<MerchantSocialMediaLink> links = Arrays.asList(
                            mapper.readValue(row[11].toString(), MerchantSocialMediaLink[].class));
                    dto.setSocialMediaLinks(links);
                } catch (Exception e) {
                    dto.setSocialMediaLinks(Collections.emptyList());
                }
            } else {
                dto.setSocialMediaLinks(Collections.emptyList());
            }
            relations.add(dto);
        }
        return relations;
    }
}
