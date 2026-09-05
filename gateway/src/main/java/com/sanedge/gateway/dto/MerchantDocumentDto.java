package com.sanedge.gateway.dto;

import java.util.List;

public class MerchantDocumentDto {

    public record CreateMerchantDocumentBody(
            int merchantId,
            String documentType,
            String documentUrl) {
    }

    public record UpdateMerchantDocumentBody(
            int merchantId,
            String documentType,
            String documentUrl,
            String note,
            String status) {
    }

    public record UpdateMerchantDocumentStatusBody(
            int merchantId,
            String note,
            String status) {
    }

    public record MerchantDocumentResponse(
            int documentId,
            int merchantId,
            String documentType,
            String documentUrl,
            String status,
            String note,
            String uploadedAt,
            String updatedAt) {
        public static MerchantDocumentResponse from(
                pb.merchant_document.MerchantDocumentCommon.MerchantDocument proto) {
            return new MerchantDocumentResponse(
                    proto.getDocumentId(),
                    proto.getMerchantId(),
                    proto.getDocumentType(),
                    proto.getDocumentUrl(),
                    proto.getStatus(),
                    proto.getNote(),
                    proto.getUploadedAt(),
                    proto.getUpdatedAt());
        }

        public static MerchantDocumentResponse from(
                pb.merchant_document.MerchantDocumentCommon.MerchantDocumentDeleteAt proto) {
            return new MerchantDocumentResponse(
                    proto.getDocumentId(),
                    proto.getMerchantId(),
                    proto.getDocumentType(),
                    proto.getDocumentUrl(),
                    proto.getStatus(),
                    proto.getNote(),
                    proto.getUploadedAt(),
                    proto.getUpdatedAt());
        }
    }

    public record FindAllMerchantDocumentsResponse(
            List<MerchantDocumentResponse> data,
            String status,
            String message) {
        public static FindAllMerchantDocumentsResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponsePaginationMerchantDocument proto) {
            return new FindAllMerchantDocumentsResponse(
                    proto.getDataList().stream().map(MerchantDocumentResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }

        public static FindAllMerchantDocumentsResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponsePaginationMerchantDocumentAt proto) {
            return new FindAllMerchantDocumentsResponse(
                    proto.getDataList().stream().map(MerchantDocumentResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record FindByIdMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static FindByIdMerchantDocumentResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto) {
            return new FindByIdMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record CreateMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static CreateMerchantDocumentResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto) {
            return new CreateMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record UpdateMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static UpdateMerchantDocumentResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto) {
            return new UpdateMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record TrashedMerchantDocumentResponse(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static TrashedMerchantDocumentResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto) {
            return new TrashedMerchantDocumentResponse(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocumentDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }

        public static SimpleStatusMessageResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocumentAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record CreateRequest(
            int merchantId,
            String documentType,
            String documentUrl) {}

    public record UpdateRequest(
            int merchantId,
            String documentType,
            String documentUrl,
            String note,
            String status) {}

    public record UpdateStatusRequest(
            int merchantId,
            String note,
            String status) {}

    public record ApiResponsePaginationDocument(
            List<MerchantDocumentResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationDocument from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponsePaginationMerchantDocument proto) {
            return new ApiResponsePaginationDocument(
                    proto.getDataList().stream().map(MerchantDocumentResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }

        public static ApiResponsePaginationDocument from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponsePaginationMerchantDocumentAt proto) {
            return new ApiResponsePaginationDocument(
                    proto.getDataList().stream().map(MerchantDocumentResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record ApiResponseDocument(
            MerchantDocumentResponse data,
            String status,
            String message) {
        public static ApiResponseDocument from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocument proto) {
            return new ApiResponseDocument(
                    proto.hasData() ? MerchantDocumentResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage());
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocumentDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }

        public static SimpleResponse from(
                pb.merchant_document.MerchantDocumentCommon.ApiResponseMerchantDocumentAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
