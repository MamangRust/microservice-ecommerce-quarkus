package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantDetailDto {
    public record MerchantSocialMediaLinkResponse(
            int id,
            int merchantDetailId,
            String platform,
            String url,
            String createdAt,
            String updatedAt) {
        public static MerchantSocialMediaLinkResponse from(pb.merchant_detail.MerchantDetailCommon.MerchantSocialMediaLinkResponse proto) {
            return new MerchantSocialMediaLinkResponse(
                    proto.getId(),
                    proto.getMerchantDetailId(),
                    proto.getPlatform(),
                    proto.getUrl(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record MerchantDetailResponse(
            int id,
            int merchantId,
            String displayName,
            String coverImageUrl,
            String logoUrl,
            String shortDescription,
            String websiteUrl,
            List<MerchantSocialMediaLinkResponse> socialMediaLinks,
            String createdAt,
            String updatedAt) {
        public static MerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponse proto) {
            return new MerchantDetailResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getDisplayName(),
                    proto.getCoverImageUrl(),
                    proto.getLogoUrl(),
                    proto.getShortDescription(),
                    proto.getWebsiteUrl(),
                    proto.getSocialMediaLinksList().stream().map(MerchantSocialMediaLinkResponse::from).toList(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static MerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.MerchantDetailResponseDeleteAt proto) {
            return new MerchantDetailResponse(
                    proto.getId(),
                    proto.getMerchantId(),
                    proto.getDisplayName(),
                    proto.getCoverImageUrl(),
                    proto.getLogoUrl(),
                    proto.getShortDescription(),
                    proto.getWebsiteUrl(),
                    proto.getSocialMediaLinksList().stream().map(MerchantSocialMediaLinkResponse::from).toList(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllMerchantDetailResponse(
            List<MerchantDetailResponse> data,
            String status,
            String message) {
        public static FindAllMerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.ApiResponsePaginationMerchantDetail proto) {
            return new FindAllMerchantDetailResponse(
                    proto.getDataList().stream().map(MerchantDetailResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.ApiResponsePaginationMerchantDetailDeleteAt proto) {
            return new FindAllMerchantDetailResponse(
                    proto.getDataList().stream().map(MerchantDetailResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdMerchantDetailResponse(
            MerchantDetailResponse data,
            String status,
            String message) {
        public static FindByIdMerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail proto) {
            return new FindByIdMerchantDetailResponse(
                    proto.hasData() ? MerchantDetailResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdMerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt proto) {
            return new FindByIdMerchantDetailResponse(
                    proto.hasData() ? MerchantDetailResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateMerchantDetailRequest(
            int merchantId,
            String displayName,
            String coverImageUrl,
            String logoUrl,
            String shortDescription,
            String websiteUrl) {}

    public record CreateMerchantDetailResponse(
            MerchantDetailResponse data,
            String status,
            String message) {
        public static CreateMerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail proto) {
            return new CreateMerchantDetailResponse(
                    proto.hasData() ? MerchantDetailResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateMerchantDetailRequest(
            int merchantId,
            String displayName,
            String coverImageUrl,
            String logoUrl,
            String shortDescription,
            String websiteUrl) {}

    public record UpdateMerchantDetailResponse(
            MerchantDetailResponse data,
            String status,
            String message) {
        public static UpdateMerchantDetailResponse from(pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail proto) {
            return new UpdateMerchantDetailResponse(
                    proto.hasData() ? MerchantDetailResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationDetail(
            List<MerchantDetailResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationDetail from(pb.merchant_detail.MerchantDetailCommon.ApiResponsePaginationMerchantDetail proto) {
            return new ApiResponsePaginationDetail(
                    proto.getDataList().stream().map(MerchantDetailResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationDetail from(pb.merchant_detail.MerchantDetailCommon.ApiResponsePaginationMerchantDetailDeleteAt proto) {
            return new ApiResponsePaginationDetail(
                    proto.getDataList().stream().map(MerchantDetailResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseDetail(
            MerchantDetailResponse data,
            String status,
            String message) {
        public static ApiResponseDetail from(pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetail proto) {
            return new ApiResponseDetail(
                    proto.hasData() ? MerchantDetailResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseDetail from(pb.merchant_detail.MerchantDetailCommon.ApiResponseMerchantDetailDeleteAt proto) {
            return new ApiResponseDetail(
                    proto.hasData() ? MerchantDetailResponse.from(proto.getData()) : null,
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
