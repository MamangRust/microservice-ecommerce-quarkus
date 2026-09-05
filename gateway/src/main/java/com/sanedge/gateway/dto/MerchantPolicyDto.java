package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantPolicyDto {

    public record MerchantPoliciesResponse(
            int id,
            int merchantId,
            String policyType,
            String title,
            String description,
            String createdAt,
            String updatedAt,
            String merchantName) {
        
        public static MerchantPoliciesResponse from(pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponse proto) {
            return new MerchantPoliciesResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getPolicyType(),
                    proto.getTitle(),
                    proto.getDescription(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.getMerchantName());
        }

        public static MerchantPoliciesResponse from(pb.merchant_policy.MerchantPolicyCommon.MerchantPoliciesResponseDeleteAt proto) {
            return new MerchantPoliciesResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getPolicyType(),
                    proto.getTitle(),
                    proto.getDescription(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.getMerchantName());
        }
    }

    public record ApiResponsePaginationPolicy(
            String status,
            String message,
            List<MerchantPoliciesResponse> data) {
        
        public static ApiResponsePaginationPolicy from(pb.merchant_policy.MerchantPolicyCommon.ApiResponsePaginationMerchantPolicies proto) {
            return new ApiResponsePaginationPolicy(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantPoliciesResponse::from).toList());
        }

        public static ApiResponsePaginationPolicy from(pb.merchant_policy.MerchantPolicyCommon.ApiResponsePaginationMerchantPoliciesDeleteAt proto) {
            return new ApiResponsePaginationPolicy(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.getDataList().stream().map(MerchantPoliciesResponse::from).toList());
        }
    }

    public record ApiResponsePolicy(
            String status,
            String message,
            MerchantPoliciesResponse data) {
        
        public static ApiResponsePolicy from(pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPolicies proto) {
            return new ApiResponsePolicy(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? MerchantPoliciesResponse.from(proto.getData()) : null);
        }

        public static ApiResponsePolicy from(pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt proto) {
            return new ApiResponsePolicy(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? MerchantPoliciesResponse.from(proto.getData()) : null);
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        
        public static SimpleResponse from(pb.merchant_policy.MerchantPolicyCommon.ApiResponseMerchantPoliciesDeleteAt proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }

        public static SimpleResponse from(pb.merchant.MerchantCommon.ApiResponseMerchantDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }

        public static SimpleResponse from(pb.merchant.MerchantCommon.ApiResponseMerchantAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
