package com.sanedge.merchant_detail.repository;

import java.util.Optional;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant_detail.entity.MerchantDetailsRelation;
import io.smallrye.mutiny.Uni;

public interface MerchantDetailQueryRepositoryCustom {
    Uni<PagedResult<MerchantDetailsRelation>> findAllWithSocialLinks(com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest req);

    Uni<PagedResult<MerchantDetailsRelation>> findActiveWithSocialLinks(com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest req);

    Uni<PagedResult<MerchantDetailsRelation>> findTrashedWithSocialLinks(com.sanedge.merchant_detail.domain.requests.FindAllMerchantRequest req);

    Uni<Optional<MerchantDetailsRelation>> findByIdWithSocialLinks(Long merchantDetailId);
}