package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantAwardDto {
    public record MerchantAwardResponse(
            int id,
            int merchantId,
            String title,
            String description,
            String issuedBy,
            String issueDate,
            String expiryDate,
            String certificateUrl,
            String createdAt,
            String updatedAt,
            String merchantName) {
        public static MerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.MerchantAwardResponse proto) {
            return new MerchantAwardResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getTitle(),
                    proto.getDescription(),
                    proto.getIssuedBy(),
                    proto.getIssueDate(),
                    proto.getExpiryDate(),
                    proto.getCertificateUrl(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.getMerchantName()
            );
        }
        public static MerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.MerchantAwardResponseDeleteAt proto) {
            return new MerchantAwardResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getTitle(),
                    proto.getDescription(),
                    proto.getIssuedBy(),
                    proto.getIssueDate(),
                    proto.getExpiryDate(),
                    proto.getCertificateUrl(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt(),
                    proto.getMerchantName()
            );
        }
    }

    public record FindAllMerchantAwardResponse(
            List<MerchantAwardResponse> data,
            String status,
            String message) {
        public static FindAllMerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponsePaginationMerchantAward proto) {
            return new FindAllMerchantAwardResponse(
                    proto.getDataList().stream().map(MerchantAwardResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponsePaginationMerchantAwardDeleteAt proto) {
            return new FindAllMerchantAwardResponse(
                    proto.getDataList().stream().map(MerchantAwardResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdMerchantAwardResponse(
            MerchantAwardResponse data,
            String status,
            String message) {
        public static FindByIdMerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward proto) {
            return new FindByIdMerchantAwardResponse(
                    proto.hasData() ? MerchantAwardResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdMerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt proto) {
            return new FindByIdMerchantAwardResponse(
                    proto.hasData() ? MerchantAwardResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateMerchantAwardRequest(
            int merchantId,
            String title,
            String description,
            String issuedBy,
            String issueDate,
            String expiryDate,
            String certificateUrl) {}

    public record CreateMerchantAwardResponse(
            MerchantAwardResponse data,
            String status,
            String message) {
        public static CreateMerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward proto) {
            return new CreateMerchantAwardResponse(
                    proto.hasData() ? MerchantAwardResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateMerchantAwardRequest(
            int merchantId,
            String title,
            String description,
            String issuedBy,
            String issueDate,
            String expiryDate,
            String certificateUrl) {}

    public record UpdateMerchantAwardResponse(
            MerchantAwardResponse data,
            String status,
            String message) {
        public static UpdateMerchantAwardResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward proto) {
            return new UpdateMerchantAwardResponse(
                    proto.hasData() ? MerchantAwardResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponsePaginationMerchantAwardDeleteAt proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationAward(
            List<MerchantAwardResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationAward from(pb.merchant_award.MerchantAwardCommon.ApiResponsePaginationMerchantAward proto) {
            return new ApiResponsePaginationAward(
                    proto.getDataList().stream().map(MerchantAwardResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationAward from(pb.merchant_award.MerchantAwardCommon.ApiResponsePaginationMerchantAwardDeleteAt proto) {
            return new ApiResponsePaginationAward(
                    proto.getDataList().stream().map(MerchantAwardResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseAward(
            MerchantAwardResponse data,
            String status,
            String message) {
        public static ApiResponseAward from(pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAward proto) {
            return new ApiResponseAward(
                    proto.hasData() ? MerchantAwardResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseAward from(pb.merchant_award.MerchantAwardCommon.ApiResponseMerchantAwardDeleteAt proto) {
            return new ApiResponseAward(
                    proto.hasData() ? MerchantAwardResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.merchant.MerchantCommon.ApiResponseMerchantDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.merchant.MerchantCommon.ApiResponseMerchantAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
