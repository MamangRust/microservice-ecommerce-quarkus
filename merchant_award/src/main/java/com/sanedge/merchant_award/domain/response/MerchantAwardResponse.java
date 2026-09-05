package com.sanedge.merchant_award.domain.response;

import com.sanedge.merchant_award.entity.MerchantCertificationAndAward;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAwardResponse {

    private Long id;
    private Integer merchantId;
    private String title;
    private String description;
    private String issuedBy;
    private String issueDate;
    private String expiryDate;
    private String certificateUrl;
    private String createdAt;
    private String updatedAt;

    public static MerchantAwardResponse from(MerchantCertificationAndAward entity) {
        return MerchantAwardResponse.builder()
                .id(entity.id)
                .merchantId(entity.getMerchantId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .issuedBy(entity.getIssuedBy())
                .issueDate(entity.getIssueDate() != null ? entity.getIssueDate().toString() : null)
                .expiryDate(entity.getExpiryDate() != null ? entity.getExpiryDate().toString() : null)
                .certificateUrl(entity.getCertificateUrl())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }
}