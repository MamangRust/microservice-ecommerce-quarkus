package com.sanedge.merchant_business.entity;

import com.sanedge.merchant_business.domain.requests.CreateMerchantBusinessRequest;
import com.sanedge.merchant_business.domain.requests.UpdateMerchantBusinessRequest;

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
@Table(name = "merchant_business_information")
public class MerchantBusinessInformation extends BaseModel {

    @Column(name = "merchant_id", nullable = false)
    private Integer merchantId;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "number_of_employees")
    private Integer numberOfEmployees;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    public static MerchantBusinessInformation fromCreateRequest(CreateMerchantBusinessRequest req) {
        MerchantBusinessInformation mbi = new MerchantBusinessInformation();
        mbi.setMerchantId(req.getMerchantId());
        mbi.setBusinessType(req.getBusinessType());
        mbi.setTaxId(req.getTaxId());
        mbi.setEstablishedYear(req.getEstablishedYear());
        mbi.setNumberOfEmployees(req.getNumberOfEmployees());
        mbi.setWebsiteUrl(req.getWebsiteUrl());
        return mbi;
    }

    public static MerchantBusinessInformation fromUpdateRequest(UpdateMerchantBusinessRequest req) {
        MerchantBusinessInformation mbi = new MerchantBusinessInformation();
        mbi.id = req.getMerchantBusinessInfoId().longValue();
        mbi.setBusinessType(req.getBusinessType());
        mbi.setTaxId(req.getTaxId());
        mbi.setEstablishedYear(req.getEstablishedYear());
        mbi.setNumberOfEmployees(req.getNumberOfEmployees());
        mbi.setWebsiteUrl(req.getWebsiteUrl());
        return mbi;
    }
}
