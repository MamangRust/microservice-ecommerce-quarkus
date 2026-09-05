package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantDto {

    public record MerchantResponse(
            int id,
            int userId,
            String name,
            String description,
            String address,
            String contactEmail,
            String contactPhone,
            String status,
            String createdAt,
            String updatedAt) {
        public static MerchantResponse from(pb.merchant.MerchantCommon.MerchantResponse proto) {
            return new MerchantResponse(
                    proto.getId(),
                    proto.getUserId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getAddress(),
                    proto.getContactEmail(),
                    proto.getContactPhone(),
                    proto.getStatus(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static MerchantResponse from(pb.merchant.MerchantCommon.MerchantResponseDeleteAt proto) {
            return new MerchantResponse(
                    proto.getId(),
                    proto.getUserId(),
                    proto.getName(),
                    proto.getDescription(),
                    proto.getAddress(),
                    proto.getContactEmail(),
                    proto.getContactPhone(),
                    proto.getStatus(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllMerchantResponse(
            List<MerchantResponse> data,
            String status,
            String message) {
        public static FindAllMerchantResponse from(pb.merchant.MerchantCommon.ApiResponsePaginationMerchant proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllMerchantResponse from(pb.merchant.MerchantCommon.ApiResponsePaginationMerchantDeleteAt proto) {
            return new FindAllMerchantResponse(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static FindByIdMerchantResponse from(pb.merchant.MerchantCommon.ApiResponseMerchant proto) {
            return new FindByIdMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdMerchantResponse from(pb.merchant.MerchantCommon.ApiResponseMerchantDeleteAt proto) {
            return new FindByIdMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateMerchantRequest(
            int userId,
            String name,
            String description,
            String address,
            String contactEmail,
            String contactPhone,
            String status) {}

    public record CreateMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static CreateMerchantResponse from(pb.merchant.MerchantCommon.ApiResponseMerchant proto) {
            return new CreateMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateMerchantRequest(
            int userId,
            String name,
            String description,
            String address,
            String contactEmail,
            String contactPhone,
            String status) {}

    public record UpdateMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static UpdateMerchantResponse from(pb.merchant.MerchantCommon.ApiResponseMerchant proto) {
            return new UpdateMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TrashedMerchantResponse(
            MerchantResponse data,
            String status,
            String message) {
        public static TrashedMerchantResponse from(pb.merchant.MerchantCommon.ApiResponseMerchantDeleteAt proto) {
            return new TrashedMerchantResponse(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationMerchant(
            List<MerchantResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationMerchant from(pb.merchant.MerchantCommon.ApiResponsePaginationMerchant proto) {
            return new ApiResponsePaginationMerchant(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseMerchant(
            MerchantResponse data,
            String status,
            String message) {
        public static ApiResponseMerchant from(pb.merchant.MerchantCommon.ApiResponseMerchant proto) {
            return new ApiResponseMerchant(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationMerchantDeleteAt(
            List<MerchantResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationMerchantDeleteAt from(pb.merchant.MerchantCommon.ApiResponsePaginationMerchantDeleteAt proto) {
            return new ApiResponsePaginationMerchantDeleteAt(
                    proto.getDataList().stream().map(MerchantResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseMerchantDeleteAt(
            MerchantResponse data,
            String status,
            String message) {
        public static ApiResponseMerchantDeleteAt from(pb.merchant.MerchantCommon.ApiResponseMerchantDeleteAt proto) {
            return new ApiResponseMerchantDeleteAt(
                    proto.hasData() ? MerchantResponse.from(proto.getData()) : null,
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
