package com.sanedge.order_item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.order_item.domain.requests.FindAllOrderItemRequest;
import com.sanedge.order_item.entity.OrderItem;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class OrderItemRepositoryTest {

    @Inject
    OrderItemRepository orderItemRepository;

    private Uni<OrderItem> createAndPersistItem(Integer orderId, Integer productId) {
        OrderItem o = new OrderItem();
        o.setOrderId(orderId);
        o.setProductId(productId);
        o.setPrice(15000);
        o.setQuantity(2);
        o.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        o.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return orderItemRepository.persist(o).replaceWith(o);
    }

    @Test
    @WithSession
    Uni<Void> testCreateAndFindById() {
        return createAndPersistItem(100, 1)
                .invoke(saved -> {
                    assertThat(saved).isNotNull();
                    assertThat(saved.id).isNotNull();
                })
                .chain(saved -> orderItemRepository.findById(saved.id))
                .invoke(found -> assertThat(found).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindByOrderId() {
        return Uni.join().all(
                createAndPersistItem(100, 1),
                createAndPersistItem(100, 2),
                createAndPersistItem(200, 3))
                .andCollectFailures()
                .chain(ignored -> orderItemRepository.findOrderItemByOrder(100L))
                .invoke(result -> assertThat(result).hasSize(2))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testFindAllItemsPaged() {
        return Uni.join().all(
                createAndPersistItem(100, 0),
                createAndPersistItem(100, 1),
                createAndPersistItem(100, 2),
                createAndPersistItem(100, 3),
                createAndPersistItem(100, 4))
                .andCollectFailures()
                .chain(ignored -> {
                    FindAllOrderItemRequest req = new FindAllOrderItemRequest();
                    req.setPage(1);
                    req.setPageSize(10);
                    return orderItemRepository.findOrderItems(req);
                })
                .invoke(result -> assertThat(result.getData()).hasSize(5))
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testTrashOrderItem() {
        return createAndPersistItem(100, 1)
                .chain(saved -> orderItemRepository.trash(saved.id))
                .invoke(trashed -> {
                    assertThat(trashed).isNotNull();
                    assertThat(trashed.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testRestoreOrderItem() {
        return createAndPersistItem(100, 1)
                .chain(saved -> orderItemRepository.trash(saved.id)
                        .chain(ignored -> orderItemRepository.restore(saved.id)))
                .invoke(restored -> {
                    assertThat(restored).isNotNull();
                    assertThat(restored.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> testDeletePermanent() {
        return createAndPersistItem(100, 1)
                .chain(saved -> orderItemRepository.trash(saved.id)
                        .chain(ignored -> orderItemRepository.deletePermanent(saved.id)))
                .invoke(deleted -> assertThat(deleted).isNotNull())
                .replaceWithVoid();
    }
}
