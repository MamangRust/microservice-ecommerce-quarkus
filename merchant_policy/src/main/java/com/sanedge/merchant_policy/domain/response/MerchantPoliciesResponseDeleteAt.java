package com.sanedge.merchant_policy.domain.response;

import com.sanedge.merchant_policy.entity.MerchantPolicy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantPoliciesResponseDeleteAt {
    private Long id;
    private Integer merchantId;
    private String policyType;
    private String title;
    private String description;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static MerchantPoliciesResponseDeleteAt from(MerchantPolicy entity) {
        return MerchantPoliciesResponseDeleteAt.builder()
                .id(entity.id)
                .merchantId(entity.getMerchantId())
                .policyType(entity.getPolicyType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .deletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toString() : null)
                .build();
    }
}