package com.sanedge.merchant_business.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant_business.entity.MerchantBusinessInformation;
import com.sanedge.merchant_business.domain.requests.FindAllMerchantRequest;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantBusinessQueryRepository implements PanacheRepository<MerchantBusinessInformation> {

    public Uni<PagedResult<MerchantBusinessInformation>> findMerchantBusinessInformation(FindAllMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NULL
                    AND (
LOWER(businessType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(taxId) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(websiteUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<MerchantBusinessInformation>> findActiveMerchantBusinessInformation(FindAllMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NULL
                    AND (
LOWER(businessType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(taxId) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(websiteUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<MerchantBusinessInformation>> findTrashedMerchantBusinessInformation(FindAllMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        var query = """
                    deletedAt IS NOT NULL
                    AND (
LOWER(businessType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(taxId) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(websiteUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<MerchantBusinessInformation> findMerchantBusinessInformationById(Long merchantBusinessInfoId) {
        return find("id = ?1 AND deletedAt IS NULL", merchantBusinessInfoId).firstResult();
    }
}
