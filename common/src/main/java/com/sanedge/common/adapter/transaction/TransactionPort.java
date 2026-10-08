package com.sanedge.common.adapter.transaction;

import com.sanedge.common.adapter.model.Transaction;
import com.sanedge.common.adapter.support.Paged;

import io.smallrye.mutiny.Uni;

/**
 * Port transaction: baca + tulis (purge) + enumerasi. Setara
 * {@code transaction.QueryRepository}/{@code CommandRepository}/{@code BulkRepository}
 * di Go.
 */
public interface TransactionPort {

    Uni<Transaction> findById(int transactionId);

    /** Cari transaksi berdasarkan order (dipakai untuk memastikan order belum dibayar). */
    Uni<Transaction> findByOrderId(int orderId);

    Uni<Boolean> deleteByOrderIdPermanent(int orderId);

    Uni<Boolean> deleteAll();

    Uni<Paged<Transaction>> findAll(int page, int pageSize);
}
