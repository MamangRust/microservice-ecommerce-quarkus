package com.sanedge.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.order.domain.requests.FindAllOrderByMerchantRequest;
import com.sanedge.order.domain.requests.FindAllOrderRequest;
import com.sanedge.order.entity.Order;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class OrderRepositoryTest {

    @Inject
    OrderQueryRepository orderQueryRepo;

    @Inject
    OrderCommandRepository orderCommandRepo;

    private Uni<Long> persistOrder(int totalPrice, int merchantId) {
        Order order = new Order();
        order.setTotalPrice(totalPrice);
        order.setMerchantId(merchantId);
        return orderQueryRepo.persist(order).map(Order::getId);
    }

    private Uni<Void> clean() {
        return orderQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllOrderRequest findAllReq(int page, int size, String search) {
        FindAllOrderRequest r = new FindAllOrderRequest();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    private FindAllOrderByMerchantRequest findAllByMerchantReq(int merchantId, int page, int size, String search) {
        FindAllOrderByMerchantRequest r = new FindAllOrderByMerchantRequest();
        r.setMerchantId(merchantId);
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persistOrder(100000, 1))
                .chain(id -> orderQueryRepo.findById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getTotalPrice()).isEqualTo(100000);
                    assertThat(found.getMerchantId()).isEqualTo(1);
                    assertThat(found.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindOrderById() {
        return clean()
                .chain(() -> persistOrder(200000, 2))
                .chain(id -> orderQueryRepo.findOrderById(id))
                .invoke(opt -> {
                    assertThat(opt).isPresent();
                    assertThat(opt.get().getTotalPrice()).isEqualTo(200000);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindOrderByIdReturnsEmptyIfNotFound() {
        return clean()
                .chain(() -> orderQueryRepo.findOrderById(999999L))
                .invoke(opt -> assertThat(opt).isEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindOrderByIdReturnsEmptyIfTrashed() {
        return clean()
                .chain(() -> persistOrder(300000, 1))
                .chain(id -> orderCommandRepo.trashed(id)
                        .chain(() -> orderQueryRepo.findOrderById(id)))
                .invoke(opt -> assertThat(opt).isEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> orderQueryRepo.findById(999999L))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashOrder() {
        return clean()
                .chain(() -> persistOrder(400000, 1))
                .chain(id -> orderCommandRepo.trashed(id))
                .invoke(t -> {
                    assertThat(t).isNotNull();
                    assertThat(t.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashOrderReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persistOrder(500000, 1))
                .chain(id -> orderCommandRepo.trashed(id)
                        .chain(() -> orderCommandRepo.trashed(id)))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashOrderReturnsNullIfNotFound() {
        return clean()
                .chain(() -> orderCommandRepo.trashed(99999L))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreOrder() {
        return clean()
                .chain(() -> persistOrder(600000, 1))
                .chain(id -> orderCommandRepo.trashed(id)
                        .chain(() -> orderCommandRepo.restore(id)))
                .invoke(r -> {
                    assertThat(r).isNotNull();
                    assertThat(r.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreOrderReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persistOrder(700000, 1))
                .chain(id -> orderCommandRepo.restore(id))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreOrderReturnsNullIfNotFound() {
        return clean()
                .chain(() -> orderCommandRepo.restore(99999L))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persistOrder(800000, 1))
                .chain(id -> orderCommandRepo.trashed(id)
                        .chain(() -> orderCommandRepo.deletePermanent(id))
                        .chain(() -> orderQueryRepo.findOrderById(id)))
                .invoke(opt -> assertThat(opt).isEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return clean()
                .chain(() -> persistOrder(900000, 1))
                .chain(id -> orderCommandRepo.deletePermanent(id))
                .invoke(d -> assertThat(d).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persistOrder(1000000, 1)
                        .chain(id -> orderCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persistOrder(1100000, 1)
                        .chain(id -> orderCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> orderCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persistOrder(1200000, 1)
                        .chain(id -> orderCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persistOrder(1300000, 1)
                        .chain(id -> orderCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persistOrder(1400000, 1)) // active
                .chain(() -> orderCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveOrders() {
        return clean()
                .chain(() -> persistOrder(1500000, 1))
                .chain(() -> persistOrder(1600000, 1))
                .chain(() -> orderQueryRepo.findActiveOrders(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedOrdersEmpty() {
        return clean()
                .chain(() -> persistOrder(1700000, 1))
                .chain(() -> persistOrder(1800000, 1))
                .chain(() -> orderQueryRepo.findTrashedOrders(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedOrders() {
        return clean()
                .chain(() -> persistOrder(1900000, 1)
                        .chain(id -> orderCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persistOrder(2000000, 1)
                        .chain(id -> orderCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> orderQueryRepo.findTrashedOrders(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindOrdersWithSearchKeyword() {
        return clean()
                .chain(() -> persistOrder(100000, 1))   // totalPrice 100000
                .chain(() -> persistOrder(200000, 1))   // totalPrice 200000
                .chain(() -> persistOrder(300000, 1))   // totalPrice 300000
                .chain(() -> orderQueryRepo.findOrders(findAllReq(1, 10, "200")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getTotalPrice()).isEqualTo(200000);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindOrdersWithPagination() {
        return clean()
                .chain(() -> persistOrder(10, 1))
                .chain(() -> persistOrder(20, 1))
                .chain(() -> persistOrder(30, 1))
                .chain(() -> persistOrder(40, 1))
                .chain(() -> persistOrder(50, 1))
                .chain(() -> orderQueryRepo.findOrders(findAllReq(1, 2, "")))
                .map(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                    return page1;
                })
                .chain(page1 -> orderQueryRepo.findOrders(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindOrdersByMerchant() {
        return clean()
                .chain(() -> persistOrder(100, 1))
                .chain(() -> persistOrder(200, 2))
                .chain(() -> persistOrder(300, 1))
                .chain(() -> orderQueryRepo.findOrdersByMerchant(findAllByMerchantReq(1, 1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindOrdersByMerchantWithSearch() {
        return clean()
                .chain(() -> persistOrder(1000, 1))
                .chain(() -> persistOrder(2000, 1))
                .chain(() -> persistOrder(2000, 2))
                .chain(() -> orderQueryRepo.findOrdersByMerchant(findAllByMerchantReq(1, 1, 10, "2000")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getMerchantId()).isEqualTo(1);
                    assertThat(r.getData().get(0).getTotalPrice()).isEqualTo(2000);
                })
                .replaceWithVoid();
    }
}