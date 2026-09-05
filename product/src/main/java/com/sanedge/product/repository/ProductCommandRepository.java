package com.sanedge.product.repository;

import com.sanedge.product.entity.Product;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class ProductCommandRepository implements PanacheRepository<Product> {

    @WithTransaction
    public Uni<Product> trashed(Long productId) {
        return findById(productId)
                .chain(product -> {
                    if (product != null && product.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        product.setDeletedAt(Timestamp.valueOf(date));
                        return persist(product).map(v -> product);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Product> restore(Long productId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", productId).firstResult()
                .chain(product -> {
                    if (product != null) {
                        product.setDeletedAt(null);
                        return persist(product).map(v -> product);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Product> adjustStock(Long productId, Integer delta) {
        if (delta == null || delta == 0) {
            return find("id = ?1 AND deletedAt IS NULL", productId).firstResult();
        }

        return update("countInStock = countInStock + ?1 "
                + "WHERE id = ?2 AND deletedAt IS NULL AND countInStock + ?1 >= 0", delta, productId)
                .chain(updated -> updated == 0
                        ? Uni.createFrom().nullItem()
                        : find("id = ?1 AND deletedAt IS NULL", productId).firstResult());
    }

    @WithTransaction
    public Uni<Product> deletePermanent(Long productId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", productId).firstResult()
                .chain(product -> {
                    if (product != null) {
                        return delete(product).map(v -> product);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }
}