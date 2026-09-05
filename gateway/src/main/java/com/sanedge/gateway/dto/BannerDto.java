package com.sanedge.gateway.dto;

import java.util.List;

public class BannerDto {

    public record BannerResponse(
            int bannerId,
            String name,
            String startDate,
            String endDate,
            String startTime,
            String endTime,
            boolean isActive,
            String createdAt,
            String updatedAt) {
        public static BannerResponse from(pb.banner.BannerCommon.BannerResponse proto) {
            return new BannerResponse(
                    proto.getBannerId(),
                    proto.getName(),
                    proto.getStartDate(),
                    proto.getEndDate(),
                    proto.getStartTime(),
                    proto.getEndTime(),
                    proto.getIsActive(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static BannerResponse from(pb.banner.BannerCommon.BannerResponseDeleteAt proto) {
            return new BannerResponse(
                    proto.getBannerId(),
                    proto.getName(),
                    proto.getStartDate(),
                    proto.getEndDate(),
                    proto.getStartTime(),
                    proto.getEndTime(),
                    proto.getIsActive(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record ApiResponsePaginationBanner(
            List<BannerResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationBanner from(pb.banner.BannerCommon.ApiResponsePaginationBanner proto) {
            return new ApiResponsePaginationBanner(
                    proto.getDataList().stream().map(BannerResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponsePaginationBanner from(pb.banner.BannerCommon.ApiResponsePaginationBannerDeleteAt proto) {
            return new ApiResponsePaginationBanner(
                    proto.getDataList().stream().map(BannerResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseBanner(
            BannerResponse data,
            String status,
            String message) {
        public static ApiResponseBanner from(pb.banner.BannerCommon.ApiResponseBanner proto) {
            return new ApiResponseBanner(
                    proto.hasData() ? BannerResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static ApiResponseBanner from(pb.banner.BannerCommon.ApiResponseBannerDeleteAt proto) {
            return new ApiResponseBanner(
                    proto.hasData() ? BannerResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.banner.BannerCommon.ApiResponseBannerDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.banner.BannerCommon.ApiResponseBannerAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
