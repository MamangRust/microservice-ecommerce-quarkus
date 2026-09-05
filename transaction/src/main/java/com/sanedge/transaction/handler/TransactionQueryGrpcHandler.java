package com.sanedge.transaction.handler;

import java.util.stream.Collectors;

import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.service.TransactionQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.MutinyTransactionQueryServiceGrpc;
import pb.transaction.TransactionCommon.ApiResponsePaginationTransaction;
import pb.transaction.TransactionCommon.ApiResponsePaginationTransactionDeleteAt;
import pb.transaction.TransactionCommon.ApiResponseTransaction;
import pb.transaction.TransactionCommon.FindByIdTransactionRequest;
import pb.transaction.TransactionQuery.FindByOrderIdTransactionRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class TransactionQueryGrpcHandler extends MutinyTransactionQueryServiceGrpc.TransactionQueryServiceImplBase {

    @Inject
    TransactionQueryService transactionQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationTransaction> findAllTransactions(
            pb.transaction.TransactionQuery.FindAllTransactionRequest request) {
        FindAllTransactionRequest domainReq = new FindAllTransactionRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return transactionQueryService.findAllTransactions(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransaction.Builder builder = ApiResponsePaginationTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream().map(this::toProto).collect(Collectors.toList()));
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationTransaction> findByMerchant(
            pb.transaction.TransactionQuery.FindAllTransactionByMerchantRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        FindAllTransactionByMerchantRequest domainReq = new FindAllTransactionByMerchantRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return transactionQueryService.findByMerchant(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransaction.Builder builder = ApiResponsePaginationTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream().map(this::toProto).collect(Collectors.toList()));
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponseTransaction> findById(FindByIdTransactionRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return transactionQueryService.findById(request.getId())
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
    @WithSession
    public Uni<ApiResponseTransaction> findByOrderId(FindByOrderIdTransactionRequest request) {
        if (request.getOrderId() <= 0) {
            return IdValidator.invalid("Order id");
        }
        return transactionQueryService.findByOrderId(request.getOrderId())
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
    @WithSession
    public Uni<ApiResponsePaginationTransaction> findByActive(
            pb.transaction.TransactionQuery.FindAllTransactionRequest request) {
        FindAllTransactionRequest domainReq = new FindAllTransactionRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return transactionQueryService.findByActive(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransaction.Builder builder = ApiResponsePaginationTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream().map(this::toProtoResponse).collect(Collectors.toList()));
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationTransactionDeleteAt> findByTrashed(
            pb.transaction.TransactionQuery.FindAllTransactionRequest request) {
        FindAllTransactionRequest domainReq = new FindAllTransactionRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return transactionQueryService.findByTrashed(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationTransactionDeleteAt.Builder builder = ApiResponsePaginationTransactionDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.addAllData(apiResp.data().stream().map(this::toProto).collect(Collectors.toList()));
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
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

    private pb.transaction.TransactionCommon.TransactionResponse toProtoResponse(TransactionResponseDeleteAt r) {
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

    private pb.Api.PaginationMeta toProto(PaginationMeta m) {
        if (m == null) {
            return pb.Api.PaginationMeta.getDefaultInstance();
        }
        return pb.Api.PaginationMeta.newBuilder()
                .setCurrentPage(m.currentPage())
                .setPageSize(m.pageSize())
                .setTotalPages(m.totalPages())
                .setTotalRecords(m.totalRecords())
                .build();
    }
}
