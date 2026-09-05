package com.sanedge.transaction.handler;

import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.service.TransactionCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.MutinyTransactionCommandServiceGrpc;
import pb.transaction.TransactionCommon.ApiResponseTransaction;
import pb.transaction.TransactionCommon.ApiResponseTransactionAll;
import pb.transaction.TransactionCommon.ApiResponseTransactionDelete;
import pb.transaction.TransactionCommon.ApiResponseTransactionDeleteAt;
import pb.transaction.TransactionCommon.FindByIdTransactionRequest;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class TransactionCommandGrpcHandler
        extends MutinyTransactionCommandServiceGrpc.TransactionCommandServiceImplBase {

    @Inject
    TransactionCommandService transactionCommandService;

    @Override
    public Uni<ApiResponseTransaction> create(CreateTransactionRequest request) {
        if (request.getOrderId() <= 0) {
            return IdValidator.invalid("Order id");
        }
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        com.sanedge.transaction.domain.requests.CreateTransactionRequest domainReq = new com.sanedge.transaction.domain.requests.CreateTransactionRequest();
        domainReq.setOrderID(request.getOrderId());
        domainReq.setMerchantID(request.getMerchantId());
        domainReq.setPaymentMethod(request.getPaymentMethod());
        domainReq.setAmount(request.getAmount());
        domainReq.setPaymentStatus(request.getPaymentStatus());

        return transactionCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseTransaction.Builder builder = ApiResponseTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseTransaction> update(UpdateTransactionRequest request) {
        if (request.getTransactionId() <= 0) {
            return IdValidator.invalid("Transaction id");
        }
        if (request.getOrderId() <= 0) {
            return IdValidator.invalid("Order id");
        }
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        com.sanedge.transaction.domain.requests.UpdateTransactionRequest domainReq = new com.sanedge.transaction.domain.requests.UpdateTransactionRequest();
        domainReq.setTransactionID(request.getTransactionId());
        domainReq.setOrderID(request.getOrderId());
        domainReq.setMerchantID(request.getMerchantId());
        domainReq.setPaymentMethod(request.getPaymentMethod());
        domainReq.setAmount(request.getAmount());
        domainReq.setPaymentStatus(request.getPaymentStatus());

        return transactionCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseTransaction.Builder builder = ApiResponseTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseTransactionDeleteAt> trashedTransaction(FindByIdTransactionRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return transactionCommandService.trash(request.getId())
                .map(apiResp -> {
                    ApiResponseTransactionDeleteAt.Builder builder = ApiResponseTransactionDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseTransactionDeleteAt> restoreTransaction(FindByIdTransactionRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return transactionCommandService.restore(request.getId())
                .map(apiResp -> {
                    ApiResponseTransactionDeleteAt.Builder builder = ApiResponseTransactionDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseTransactionDelete> deleteTransactionPermanent(FindByIdTransactionRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return transactionCommandService.delete(request.getId())
                .map(apiResp -> ApiResponseTransactionDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseTransactionAll> restoreAllTransaction(com.google.protobuf.Empty request) {
        return transactionCommandService.restoreAll()
                .map(apiResp -> ApiResponseTransactionAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseTransactionDelete> deleteTransactionByOrderPermanent(FindByIdTransactionRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return transactionCommandService.deleteByOrder(request.getId())
                .map(apiResp -> ApiResponseTransactionDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseTransactionAll> deleteAllTransactionPermanent(com.google.protobuf.Empty request) {
        return transactionCommandService.deleteAll()
                .map(apiResp -> ApiResponseTransactionAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.transaction.TransactionCommon.TransactionResponse toProto(TransactionResponse r) {
        if (r == null) {
            return pb.transaction.TransactionCommon.TransactionResponse.getDefaultInstance();
        }
        pb.transaction.TransactionCommon.TransactionResponse.Builder builder = pb.transaction.TransactionCommon.TransactionResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getOrderId() != null) {
            builder.setOrderId(r.getOrderId());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getPaymentMethod() != null) {
            builder.setPaymentMethod(r.getPaymentMethod());
        }
        if (r.getAmount() != null) {
            builder.setAmount(r.getAmount());
        }
        if (r.getPaymentStatus() != null) {
            builder.setPaymentStatus(r.getPaymentStatus());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        return builder.build();
    }

    private pb.transaction.TransactionCommon.TransactionResponseDeleteAt toProto(TransactionResponseDeleteAt r) {
        if (r == null) {
            return pb.transaction.TransactionCommon.TransactionResponseDeleteAt.getDefaultInstance();
        }
        pb.transaction.TransactionCommon.TransactionResponseDeleteAt.Builder builder = pb.transaction.TransactionCommon.TransactionResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getOrderId() != null) {
            builder.setOrderId(r.getOrderId());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getPaymentMethod() != null) {
            builder.setPaymentMethod(r.getPaymentMethod());
        }
        if (r.getAmount() != null) {
            builder.setAmount(r.getAmount());
        }
        if (r.getPaymentStatus() != null) {
            builder.setPaymentStatus(r.getPaymentStatus());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt().toString()));
        }
        return builder.build();
    }
}
