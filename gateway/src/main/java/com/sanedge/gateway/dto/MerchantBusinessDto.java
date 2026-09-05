package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantBusinessDto {
    public record MerchantBusinessResponse(
            int id,
            int merchantId,
            String businessType,
            String taxId,
            int establishedYear,
            int numberOfEmployees,
            String websiteUrl,
            String merchantName,
            String createdAt,
            String updatedAt) {
        public static MerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponse proto) {
            return new MerchantBusinessResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getBusinessType(),
                    proto.getTaxId(),
                    proto.getEstablishedYear(),
                    proto.getNumberOfEmployees(),
                    proto.getWebsiteUrl(),
                    proto.getMerchantName(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static MerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.MerchantBusinessResponseDeleteAt proto) {
            return new MerchantBusinessResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getBusinessType(),
                    proto.getTaxId(),
                    proto.getEstablishedYear(),
                    proto.getNumberOfEmployees(),
                    proto.getWebsiteUrl(),
                    proto.getMerchantName(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllMerchantBusinessResponse(
            List<MerchantBusinessResponse> data,
            String status,
            String message) {
        public static FindAllMerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.ApiResponsePaginationMerchantBusiness proto) {
            return new FindAllMerchantBusinessResponse(
                    proto.getDataList().stream().map(MerchantBusinessResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.ApiResponsePaginationMerchantBusinessDeleteAt proto) {
            return new FindAllMerchantBusinessResponse(
                    proto.getDataList().stream().map(MerchantBusinessResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdMerchantBusinessResponse(
            MerchantBusinessResponse data,
            String status,
            String message) {
        public static FindByIdMerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness proto) {
            return new FindByIdMerchantBusinessResponse(
                    proto.hasData() ? MerchantBusinessResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdMerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt proto) {
            return new FindByIdMerchantBusinessResponse(
                    proto.hasData() ? MerchantBusinessResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateMerchantBusinessRequest(
            int merchantId,
            String businessType,
            String taxId,
            int establishedYear,
            int numberOfEmployees,
            String websiteUrl) {}

    public record CreateMerchantBusinessResponse(
            MerchantBusinessResponse data,
            String status,
            String message) {
        public static CreateMerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness proto) {
            return new CreateMerchantBusinessResponse(
                    proto.hasData() ? MerchantBusinessResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateMerchantBusinessRequest(
            int merchantId,
            String businessType,
            String taxId,
            int establishedYear,
            int numberOfEmployees,
            String websiteUrl) {}

    public record UpdateMerchantBusinessResponse(
            MerchantBusinessResponse data,
            String status,
            String message) {
        public static UpdateMerchantBusinessResponse from(pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness proto) {
            return new UpdateMerchantBusinessResponse(
                    proto.hasData() ? MerchantBusinessResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationBusiness(
            List<MerchantBusinessResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationBusiness from(pb.merchant_business.MerchantBusinessCommon.ApiResponsePaginationMerchantBusiness proto) {
            return new ApiResponsePaginationBusiness(
                    proto.getDataList().stream().map(MerchantBusinessResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationBusiness from(pb.merchant_business.MerchantBusinessCommon.ApiResponsePaginationMerchantBusinessDeleteAt proto) {
            return new ApiResponsePaginationBusiness(
                    proto.getDataList().stream().map(MerchantBusinessResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseBusiness(
            MerchantBusinessResponse data,
            String status,
            String message) {
        public static ApiResponseBusiness from(pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusiness proto) {
            return new ApiResponseBusiness(
                    proto.hasData() ? MerchantBusinessResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseBusiness from(pb.merchant_business.MerchantBusinessCommon.ApiResponseMerchantBusinessDeleteAt proto) {
            return new ApiResponseBusiness(
                    proto.hasData() ? MerchantBusinessResponse.from(proto.getData()) : null,
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
