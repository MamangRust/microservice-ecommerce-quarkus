package com.sanedge.gateway.dto;

import java.util.List;

public class TransactionDto {
    public record TransactionResponse(
            int id,
            int orderId,
            int merchantId,
            String paymentMethod,
            int amount,
            String paymentStatus,
            String createdAt,
            String updatedAt) {
        public static TransactionResponse from(pb.transaction.TransactionCommon.TransactionResponse proto) {
            return new TransactionResponse(
                    proto.getId(),
                    proto.getOrderId(),
                    proto.getMerchantId(),
                    proto.getPaymentMethod(),
                    proto.getAmount(),
                    proto.getPaymentStatus(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
        public static TransactionResponse from(pb.transaction.TransactionCommon.TransactionResponseDeleteAt proto) {
            return new TransactionResponse(
                    proto.getId(),
                    proto.getOrderId(),
                    proto.getMerchantId(),
                    proto.getPaymentMethod(),
                    proto.getAmount(),
                    proto.getPaymentStatus(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record FindAllTransactionResponse(
            List<TransactionResponse> data,
            String status,
            String message) {
        public static FindAllTransactionResponse from(pb.transaction.TransactionCommon.ApiResponsePaginationTransaction proto) {
            return new FindAllTransactionResponse(
                    proto.getDataList().stream().map(TransactionResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllTransactionResponse from(pb.transaction.TransactionCommon.ApiResponsePaginationTransactionDeleteAt proto) {
            return new FindAllTransactionResponse(
                    proto.getDataList().stream().map(TransactionResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdTransactionResponse(
            TransactionResponse data,
            String status,
            String message) {
        public static FindByIdTransactionResponse from(pb.transaction.TransactionCommon.ApiResponseTransaction proto) {
            return new FindByIdTransactionResponse(
                    proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindByIdTransactionResponse from(pb.transaction.TransactionCommon.ApiResponseTransactionDeleteAt proto) {
            return new FindByIdTransactionResponse(
                    proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateTransactionRequest(
            int orderId,
            int merchantId,
            String paymentMethod,
            int amount,
            String paymentStatus) {}

    public record CreateTransactionResponse(
            TransactionResponse data,
            String status,
            String message) {
        public static CreateTransactionResponse from(pb.transaction.TransactionCommon.ApiResponseTransaction proto) {
            return new CreateTransactionResponse(
                    proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateTransactionRequest(
            int orderId,
            int merchantId,
            String paymentMethod,
            int amount,
            String paymentStatus) {}

    public record UpdateTransactionResponse(
            TransactionResponse data,
            String status,
            String message) {
        public static UpdateTransactionResponse from(pb.transaction.TransactionCommon.ApiResponseTransaction proto) {
            return new UpdateTransactionResponse(
                    proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.transaction.TransactionCommon.ApiResponseTransactionDeleteAt proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.transaction.TransactionCommon.ApiResponseTransactionDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record ApiResponsePaginationTransaction(
            List<TransactionResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationTransaction from(pb.transaction.TransactionCommon.ApiResponsePaginationTransaction proto) {
            return new ApiResponsePaginationTransaction(
                    proto.getDataList().stream().map(TransactionResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponsePaginationTransactionDeleteAt(
            List<TransactionResponse> data,
            String status,
            String message) {
        public static ApiResponsePaginationTransactionDeleteAt from(pb.transaction.TransactionCommon.ApiResponsePaginationTransactionDeleteAt proto) {
            return new ApiResponsePaginationTransactionDeleteAt(
                    proto.getDataList().stream().map(TransactionResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseTransaction(
            TransactionResponse data,
            String status,
            String message) {
        public static ApiResponseTransaction from(pb.transaction.TransactionCommon.ApiResponseTransaction proto) {
            return new ApiResponseTransaction(
                    proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record ApiResponseTransactionDeleteAt(
            TransactionResponse data,
            String status,
            String message) {
        public static ApiResponseTransactionDeleteAt from(pb.transaction.TransactionCommon.ApiResponseTransactionDeleteAt proto) {
            return new ApiResponseTransactionDeleteAt(
                    proto.hasData() ? TransactionResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.transaction.TransactionCommon.ApiResponseTransactionDelete proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleResponse from(pb.transaction.TransactionCommon.ApiResponseTransactionAll proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }

    public record TransactionMonthlyAmountSuccess(
            String year,
            String month,
            int totalSuccess,
            int totalAmount) {
        public static TransactionMonthlyAmountSuccess from(pb.transaction.TransactionCommon.TransactionMonthlyAmountSuccess proto) {
            return new TransactionMonthlyAmountSuccess(proto.getYear(), proto.getMonth(), proto.getTotalSuccess(), proto.getTotalAmount());
        }
    }

    public record ApiResponseTransactionMonthAmountSuccess(
            List<TransactionMonthlyAmountSuccess> data,
            String status,
            String message) {
        public static ApiResponseTransactionMonthAmountSuccess from(pb.transaction.TransactionCommon.ApiResponseTransactionMonthAmountSuccess proto) {
            return new ApiResponseTransactionMonthAmountSuccess(
                    proto.getDataList().stream().map(TransactionMonthlyAmountSuccess::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TransactionMonthlyAmountFailed(
            String year,
            String month,
            int totalFailed,
            int totalAmount) {
        public static TransactionMonthlyAmountFailed from(pb.transaction.TransactionCommon.TransactionMonthlyAmountFailed proto) {
            return new TransactionMonthlyAmountFailed(proto.getYear(), proto.getMonth(), proto.getTotalFailed(), proto.getTotalAmount());
        }
    }

    public record ApiResponseTransactionMonthAmountFailed(
            List<TransactionMonthlyAmountFailed> data,
            String status,
            String message) {
        public static ApiResponseTransactionMonthAmountFailed from(pb.transaction.TransactionCommon.ApiResponseTransactionMonthAmountFailed proto) {
            return new ApiResponseTransactionMonthAmountFailed(
                    proto.getDataList().stream().map(TransactionMonthlyAmountFailed::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TransactionYearlyAmountSuccess(
            String year,
            int totalSuccess,
            int totalAmount) {
        public static TransactionYearlyAmountSuccess from(pb.transaction.TransactionCommon.TransactionYearlyAmountSuccess proto) {
            return new TransactionYearlyAmountSuccess(proto.getYear(), proto.getTotalSuccess(), proto.getTotalAmount());
        }
    }

    public record ApiResponseTransactionYearAmountSuccess(
            List<TransactionYearlyAmountSuccess> data,
            String status,
            String message) {
        public static ApiResponseTransactionYearAmountSuccess from(pb.transaction.TransactionCommon.ApiResponseTransactionYearAmountSuccess proto) {
            return new ApiResponseTransactionYearAmountSuccess(
                    proto.getDataList().stream().map(TransactionYearlyAmountSuccess::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TransactionYearlyAmountFailed(
            String year,
            int totalFailed,
            int totalAmount) {
        public static TransactionYearlyAmountFailed from(pb.transaction.TransactionCommon.TransactionYearlyAmountFailed proto) {
            return new TransactionYearlyAmountFailed(proto.getYear(), proto.getTotalFailed(), proto.getTotalAmount());
        }
    }

    public record ApiResponseTransactionYearAmountFailed(
            List<TransactionYearlyAmountFailed> data,
            String status,
            String message) {
        public static ApiResponseTransactionYearAmountFailed from(pb.transaction.TransactionCommon.ApiResponseTransactionYearAmountFailed proto) {
            return new ApiResponseTransactionYearAmountFailed(
                    proto.getDataList().stream().map(TransactionYearlyAmountFailed::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TransactionMonthlyMethod(
            String month,
            String paymentMethod,
            int totalTransactions,
            int totalAmount) {
        public static TransactionMonthlyMethod from(pb.transaction.TransactionCommon.TransactionMonthlyMethod proto) {
            return new TransactionMonthlyMethod(proto.getMonth(), proto.getPaymentMethod(), proto.getTotalTransactions(), proto.getTotalAmount());
        }
    }

    public record ApiResponseTransactionMonthPaymentMethod(
            List<TransactionMonthlyMethod> data,
            String status,
            String message) {
        public static ApiResponseTransactionMonthPaymentMethod from(pb.transaction.TransactionCommon.ApiResponseTransactionMonthPaymentMethod proto) {
            return new ApiResponseTransactionMonthPaymentMethod(
                    proto.getDataList().stream().map(TransactionMonthlyMethod::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TransactionYearlyMethod(
            String year,
            String paymentMethod,
            int totalTransactions,
            int totalAmount) {
        public static TransactionYearlyMethod from(pb.transaction.TransactionCommon.TransactionYearlyMethod proto) {
            return new TransactionYearlyMethod(proto.getYear(), proto.getPaymentMethod(), proto.getTotalTransactions(), proto.getTotalAmount());
        }
    }

    public record ApiResponseTransactionYearPaymentmethod(
            List<TransactionYearlyMethod> data,
            String status,
            String message) {
        public static ApiResponseTransactionYearPaymentmethod from(pb.transaction.TransactionCommon.ApiResponseTransactionYearPaymentmethod proto) {
            return new ApiResponseTransactionYearPaymentmethod(
                    proto.getDataList().stream().map(TransactionYearlyMethod::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }
}
