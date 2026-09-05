package com.sanedge.merchant_detail.entity;

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
@Table(name = "merchant_social_media_links")
public class MerchantSocialMediaLink extends BaseModel {

    @Column(name = "merchant_detail_id", nullable = false)
    private Integer merchantDetailId;

    @Column(nullable = false, length = 100)
    private String platform;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;
}
