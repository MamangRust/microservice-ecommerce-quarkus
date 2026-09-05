package com.sanedge.transaction.repository;

import java.util.Optional;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.enums.PaymentStatus;
import com.sanedge.transaction.domain.requests.FindAllTransactionRequest;
import com.sanedge.transaction.domain.requests.FindAllTransactionByMerchantRequest;

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionQueryRepository implements PanacheRepository<Transaction> {

    public Uni<PagedResult<Transaction>> findTransactions(FindAllTransactionRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Transaction> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NULL").page(page, size);
        } else {
            var query = "deletedAt IS NULL AND LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Transaction>> findActiveTransactions(FindAllTransactionRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Transaction> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NULL").page(page, size);
        } else {
            var query = "deletedAt IS NULL AND LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Transaction>> findTrashedTransactions(FindAllTransactionRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        PanacheQuery<Transaction> panacheQuery;
        if (keyword == null) {
            panacheQuery = find("deletedAt IS NOT NULL").page(page, size);
        } else {
            var query = "deletedAt IS NOT NULL AND LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<PagedResult<Transaction>> findTransactionsByMerchant(FindAllTransactionByMerchantRequest req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
        Integer merchantId = req.getMerchantId();

        PanacheQuery<Transaction> panacheQuery;
        if (keyword == null && merchantId == null) {
            panacheQuery = find("deletedAt IS NULL").page(page, size);
        } else if (keyword == null) {
            panacheQuery = find("deletedAt IS NULL AND merchantId = ?1", merchantId).page(page, size);
        } else if (merchantId == null) {
            var query = "deletedAt IS NULL AND LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword).page(page, size);
        } else {
            var query = "deletedAt IS NULL AND merchantId = ?2 "
                    + "AND LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))";
            panacheQuery = find(query, keyword, merchantId).page(page, size);
        }
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(t -> new PagedResult<>(t.getItem1(), t.getItem2().intValue()));
    }

    public Uni<Optional<Transaction>> findTransactionById(Long transactionId) {
        return find("id = ?1 AND deletedAt IS NULL", transactionId).firstResult().map(Optional::ofNullable);
    }

    public Uni<Optional<Transaction>> findByOrderId(Integer orderId) {
        return find("orderId = ?1 AND deletedAt IS NULL", orderId).firstResult().map(Optional::ofNullable);
    }

    public Uni<Optional<Transaction>> findActivePaymentByOrderId(Integer orderId) {
        return find("orderId = ?1 AND deletedAt IS NULL AND status <> ?2",
                orderId, PaymentStatus.FAILED).firstResult().map(Optional::ofNullable);
    }
}
