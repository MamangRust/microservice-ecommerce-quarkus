package com.sanedge.merchant_detail.domain.response;

import java.util.List;
import java.util.stream.Collectors;

import com.sanedge.merchant_detail.entity.MerchantDetailsRelation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDetailRelationResponseDeleteAt {
    private Long id;
    private Integer merchantId;
    private String displayName;
    private String coverImageUrl;
    private String logoUrl;
    private String shortDescription;
    private String websiteUrl;
    private List<MerchantSocialMediaLinkResponse> socialMediaLinks;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static MerchantDetailRelationResponseDeleteAt from(MerchantDetailsRelation relation) {
        return MerchantDetailRelationResponseDeleteAt.builder()
                .id((long) relation.getId())
                .merchantId(relation.getMerchantId())
                .displayName(relation.getDisplayName())
                .coverImageUrl(relation.getCoverImageUrl())
                .logoUrl(relation.getLogoUrl())
                .shortDescription(relation.getShortDescription())
                .websiteUrl(relation.getWebsiteUrl())
                .socialMediaLinks(relation.getSocialMediaLinks().stream()
                        .map(MerchantSocialMediaLinkResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(relation.getCreatedAt())
                .updatedAt(relation.getUpdatedAt())
                .deletedAt(relation.getDeletedAt())
                .build();
    }
}