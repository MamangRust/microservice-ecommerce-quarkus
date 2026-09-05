package com.sanedge.merchant_award.entity;

import java.sql.Date;

import com.sanedge.merchant_award.domain.requests.CreateMerchantAwardRequest;
import com.sanedge.merchant_award.domain.requests.UpdateMerchantAwardRequest;

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
@Table(name = "merchant_certifications_and_awards")
public class MerchantCertificationAndAward extends BaseModel {

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "issued_by", length = 255)
    private String issuedBy;

    @Column(name = "issue_date")
    private Date issueDate;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @Column(name = "certificate_url", length = 255)
    private String certificateUrl;

    public static MerchantCertificationAndAward fromCreateRequest(CreateMerchantAwardRequest req) {
        MerchantCertificationAndAward award = new MerchantCertificationAndAward();
        award.setMerchantId(req.getMerchantId());
        award.setTitle(req.getTitle());
        award.setDescription(req.getDescription());
        award.setIssuedBy(req.getIssuedBy());
        award.setIssueDate(parseDate(req.getIssueDate()));
        award.setExpiryDate(parseDate(req.getExpiryDate()));
        award.setCertificateUrl(req.getCertificateUrl());
        return award;
    }

    public void updateFromRequest(UpdateMerchantAwardRequest req) {
        this.title = req.getTitle();
        this.description = req.getDescription();
        this.issuedBy = req.getIssuedBy();
        this.issueDate = parseDate(req.getIssueDate());
        this.expiryDate = parseDate(req.getExpiryDate());
        this.certificateUrl = req.getCertificateUrl();
    }

    private static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        return Date.valueOf(dateStr);
    }
}
