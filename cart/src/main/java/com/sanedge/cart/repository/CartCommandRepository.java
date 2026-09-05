package com.sanedge.cart.repository;

import com.sanedge.cart.entity.Cart;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class CartCommandRepository implements PanacheRepository<Cart> {

    @WithTransaction
    public Uni<Boolean> deleteCartById(Long cartId) {
        return delete("id", cartId).map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteCartsByIds(List<Long> cartIds) {
        return delete("id IN ?1", cartIds).map(count -> count > 0);
    }
}
