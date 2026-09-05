package com.sanedge.merchant_policy.entity;

import com.sanedge.merchant_policy.domain.requests.CreateMerchantPolicyRequest;
import com.sanedge.merchant_policy.domain.requests.UpdateMerchantPolicyRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "merchant_policies")
public class MerchantPolicy extends BaseModel {

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(name = "policy_type", nullable = false, length = 100)
    private String policyType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    public static MerchantPolicy fromCreateRequest(CreateMerchantPolicyRequest req) {
        MerchantPolicy entity = new MerchantPolicy();
        entity.setMerchantId(req.getMerchantId());
        entity.setPolicyType(req.getPolicyType());
        entity.setTitle(req.getTitle());
        entity.setDescription(req.getDescription());
        return entity;
    }

    public void updateFromRequest(UpdateMerchantPolicyRequest req) {
        this.setPolicyType(req.getPolicyType());
        this.setTitle(req.getTitle());
        this.setDescription(req.getDescription());
    }
}