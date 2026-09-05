package com.sanedge.order.repository;

import com.sanedge.order.entity.ShippingAddress;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@ApplicationScoped
public class ShippingAddressCommandRepository implements PanacheRepository<ShippingAddress> {

    @WithTransaction
    public Uni<ShippingAddress> trashed(Long shippingAddressId) {
        return findById(shippingAddressId)
                .chain(address -> {
                    if (address != null && address.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        address.setDeletedAt(Timestamp.valueOf(date));
                        return persist(address).map(v -> address);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<ShippingAddress> restore(Long shippingAddressId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", shippingAddressId).firstResult()
                .chain(address -> {
                    if (address != null) {
                        address.setDeletedAt(null);
                        return persist(address).map(v -> address);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<ShippingAddress> deletePermanent(Long shippingAddressId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", shippingAddressId).firstResult()
                .chain(address -> {
                    if (address != null) {
                        return delete(address).map(v -> address);
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
