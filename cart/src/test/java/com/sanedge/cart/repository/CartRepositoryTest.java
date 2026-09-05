package com.sanedge.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import com.sanedge.cart.domain.requests.FindAllCartsRequest;
import com.sanedge.cart.entity.Cart;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class CartRepositoryTest {

    @Inject
    CartQueryRepository cartQueryRepo;

    @Inject
    CartCommandRepository cartCommandRepo;

    private Uni<Cart> createAndPersistCart(Integer userId, String name, Integer price) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setName(name);
        cart.setPrice(price);

        return cartQueryRepo.persist(cart).replaceWith(cart);
    }

    private FindAllCartsRequest buildRequest(Integer userId, int page, int pageSize, String search) {
        FindAllCartsRequest req = new FindAllCartsRequest();
        req.setUserId(userId);
        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(search);
        return req;
    }

    @Test
    @WithSession
    Uni<Void> testFindCartsByUser_Success() {
        return Uni.join().all(
                createAndPersistCart(1, "Laptop", 15000000),
                createAndPersistCart(1, "Mouse", 500000))
                .andCollectFailures()
                .chain(ignored -> cartQueryRepo.findCartsByUser(buildRequest(1, 1, 10, null)))
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindCartsByUser_FiltersByUserId() {
        return Uni.join().all(
                createAndPersistCart(1, "Item A", 1000),
                createAndPersistCart(2, "Item B", 2000))
                .andCollectFailures()
                .chain(ignored -> cartQueryRepo.findCartsByUser(buildRequest(1, 1, 10, null)))
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Item A");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindCartsByUser_WithSearchKeywordByName() {
        return Uni.join().all(
                createAndPersistCart(1, "Gaming Laptop", 15000000),
                createAndPersistCart(1, "Office Laptop", 10000000),
                createAndPersistCart(1, "Gaming Mouse", 1500000))
                .andCollectFailures()
                .chain(ignored -> cartQueryRepo.findCartsByUser(buildRequest(1, 1, 10, "Gaming")))
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(2);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindCartsByUser_WithSearchKeywordByPrice() {
        return Uni.join().all(
                createAndPersistCart(1, "Item 15K", 15000),
                createAndPersistCart(1, "Item 20K", 20000))
                .andCollectFailures()
                .chain(ignored -> cartQueryRepo.findCartsByUser(buildRequest(1, 1, 10, "15000")))
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(1);
                    assertThat(result.getData().get(0).getName()).isEqualTo("Item 15K");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindCartsByUser_WithPagination() {
        return Uni.join().all(
                createAndPersistCart(1, "Item 1", 1000),
                createAndPersistCart(1, "Item 2", 2000),
                createAndPersistCart(1, "Item 3", 3000))
                .andCollectFailures()
                .chain(ignored -> cartQueryRepo.findCartsByUser(buildRequest(1, 1, 2, null)))
                .invoke(result -> {
                    assertThat(result.getData()).hasSize(2);
                    assertThat(result.getTotalRecords()).isEqualTo(3);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindCartsByUser_ReturnsEmptyWhenNoMatch() {
        return createAndPersistCart(1, "Laptop", 15000000)
                .chain(ignored -> cartQueryRepo.findCartsByUser(buildRequest(1, 1, 10, "Mouse")))
                .invoke(result -> {
                    assertThat(result.getData()).isEmpty();
                    assertThat(result.getTotalRecords()).isZero();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteCartById_Success() {
        return createAndPersistCart(1, "To Delete", 1000)
                .chain(saved -> cartCommandRepo.deleteCartById(saved.id)
                        .invoke(deleted -> assertThat(deleted).isTrue())
                        .chain(ignored -> cartQueryRepo.findById(saved.id)))
                .invoke(checkDb -> assertThat(checkDb).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteCartById_ReturnsFalseIfNotFound() {
        return cartCommandRepo.deleteCartById(99999L)
                .invoke(deleted -> assertThat(deleted).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteCartsByIds_Success() {
        return Uni.join().all(
                createAndPersistCart(1, "Bulk 1", 1000),
                createAndPersistCart(1, "Bulk 2", 2000),
                createAndPersistCart(1, "Bulk 3", 3000))
                .andCollectFailures()
                .chain(carts -> {
                    List<Long> idsToDelete = List.of(carts.get(0).id, carts.get(1).id);
                    return cartCommandRepo.deleteCartsByIds(idsToDelete)
                            .invoke(deleted -> assertThat(deleted).isTrue())
                            .replaceWith(carts.get(2).id);
                })
                .chain(remainingId -> cartQueryRepo.findById(remainingId))
                .invoke(remainingCart -> {
                    assertThat(remainingCart).isNotNull();
                    assertThat(remainingCart.getName()).isEqualTo("Bulk 3");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeleteCartsByIds_ReturnsFalseIfNoneFound() {
        return cartCommandRepo.deleteCartsByIds(List.of(99998L, 99999L))
                .invoke(deleted -> assertThat(deleted).isFalse())
                .replaceWithVoid();
    }
}
