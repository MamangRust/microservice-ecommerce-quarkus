package com.sanedge.merchant.domain.response;

import com.sanedge.merchant.entity.Merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantResponseDeleteAt {
    private Long id;
    private Integer userId;
    private String name;
    private String description;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static MerchantResponseDeleteAt from(Merchant merchant) {
        return MerchantResponseDeleteAt.builder()
                .id(merchant.getMerchantId())
                .userId(merchant.getUserId())
                .name(merchant.getName())
                .description(merchant.getDescription())
                .address(merchant.getAddress())
                .contactEmail(merchant.getContactEmail())
                .contactPhone(merchant.getContactPhone())
                .status(merchant.getStatus() != null ? merchant.getStatus().name() : null)
                .createdAt(merchant.getCreatedAt() != null ? merchant.getCreatedAt().toString() : null)
                .updatedAt(merchant.getUpdatedAt() != null ? merchant.getUpdatedAt().toString() : null)
                .deletedAt(merchant.getDeletedAt() != null ? merchant.getDeletedAt().toString() : null)
                .build();
    }
}