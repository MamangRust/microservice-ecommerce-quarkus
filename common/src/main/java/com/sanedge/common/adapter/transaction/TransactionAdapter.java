package com.sanedge.common.adapter.transaction;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Empty;
import com.sanedge.common.adapter.model.Transaction;
import com.sanedge.common.adapter.support.Paged;
import com.sanedge.common.adapter.support.ProtoTime;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.transaction.MutinyTransactionCommandServiceGrpc.MutinyTransactionCommandServiceStub;
import pb.transaction.MutinyTransactionQueryServiceGrpc.MutinyTransactionQueryServiceStub;
import pb.transaction.TransactionCommon.FindByIdTransactionRequest;
import pb.transaction.TransactionCommon.TransactionResponse;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;

@ApplicationScoped
public class TransactionAdapter implements TransactionPort {

    @GrpcClient("transaction")
    MutinyTransactionQueryServiceStub query;

    @GrpcClient("transaction")
    MutinyTransactionCommandServiceStub command;

    @Override
    public Uni<Transaction> findById(int transactionId) {
        return query.findById(FindByIdTransactionRequest.newBuilder().setId(transactionId).build())
                .map(resp -> toModel(resp.getData()));
    }

    @Override
    public Uni<Transaction> findByOrderId(int orderId) {
        return query.findByOrderId(pb.transaction.TransactionQuery.FindByOrderIdTransactionRequest.newBuilder()
                .setOrderId(orderId)
                .build())
                .map(resp -> toModel(resp.getData()));
    }

    @Override
    public Uni<Boolean> deleteByOrderIdPermanent(int orderId) {
        return command.deleteTransactionByOrderPermanent(FindByIdTransactionRequest.newBuilder().setId(orderId).build())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Boolean> deleteAll() {
        return command.deleteAllTransactionPermanent(Empty.getDefaultInstance())
                .map(resp -> "success".equals(resp.getStatus()));
    }

    @Override
    public Uni<Paged<Transaction>> findAll(int page, int pageSize) {
        return query.findAllTransactions(FindAllTransactionRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build())
                .map(resp -> {
                    List<Transaction> items = new ArrayList<>(resp.getDataList().size());
                    for (TransactionResponse t : resp.getDataList()) {
                        items.add(toModel(t));
                    }
                    int total = resp.hasPagination() ? resp.getPagination().getTotalRecords() : items.size();
                    return Paged.of(items, total);
                });
    }

    static Transaction toModel(TransactionResponse t) {
        if (t == null) {
            return null;
        }
        return new Transaction(
                t.getId(),
                t.getOrderId(),
                t.getMerchantId(),
                t.getPaymentMethod(),
                t.getAmount(),
                t.getPaymentStatus(),
                ProtoTime.parse(t.getCreatedAt()),
                ProtoTime.parse(t.getUpdatedAt()));
    }
}
