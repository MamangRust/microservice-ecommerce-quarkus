package com.sanedge.merchant_detail.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDetailsRelation {
    private int id;
    private int merchantId;
    private String displayName;
    private String coverImageUrl;
    private String logoUrl;
    private String shortDescription;
    private String websiteUrl;
    private List<MerchantSocialMediaLink> socialMediaLinks;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

}