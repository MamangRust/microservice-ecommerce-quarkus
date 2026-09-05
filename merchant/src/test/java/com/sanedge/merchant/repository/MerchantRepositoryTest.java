package com.sanedge.merchant.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.Status;
import com.sanedge.merchant.domain.requests.FindAllMerchants;
import com.sanedge.merchant.entity.Merchant;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class MerchantRepositoryTest {

    @Inject
    MerchantQueryRepository merchantQueryRepo;

    @Inject
    MerchantCommandRepository merchantCommandRepo;

    private static long idCounter = 1;

    private Uni<Long> persist(String name, String description, String address, String email, String phone, Status status) {
        Merchant m = new Merchant();
        long merchantId = idCounter++;
        m.setMerchantId(merchantId);
        m.setName(name);
        m.setDescription(description);
        m.setAddress(address);
        m.setContactEmail(email);
        m.setContactPhone(phone);
        m.setStatus(status);
        return merchantQueryRepo.persist(m).map(ignore -> merchantId);
    }

    private Uni<Long> persist(String name) {
        return persist(name, "desc " + name, "address " + name, name + "@mail.com", "08123456789", Status.SUCCESS);
    }


    private Uni<Void> clean() {
        return merchantQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllMerchants findAllReq(int page, int size, String search) {
        FindAllMerchants r = new FindAllMerchants();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindByMerchantId() {
        return clean()
                .chain(() -> persist("Toko Makmur"))
                .chain(id -> merchantQueryRepo.findMerchantById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getName()).isEqualTo("Toko Makmur");
                    assertThat(found.getStatus()).isEqualTo(Status.SUCCESS);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantById_ReturnsNullIfNotFound() {
        return clean()
                .chain(() -> merchantQueryRepo.findMerchantById(999999L))
                .invoke(result -> assertThat(result).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantById_ReturnsNullIfTrashed() {
        return clean()
                .chain(() -> persist("Toko Sementara"))
                .chain(id -> merchantCommandRepo.trashed(id)
                        .chain(() -> merchantQueryRepo.findMerchantById(id)))
                .invoke(result -> assertThat(result).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testExistsByName_ReturnsTrue() {
        return clean()
                .chain(() -> persist("UniqueMart"))
                .chain(() -> merchantQueryRepo.existsByName("UniqueMart"))
                .invoke(exists -> assertThat(exists).isTrue())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testExistsByName_ReturnsFalse() {
        return clean()
                .chain(() -> merchantQueryRepo.existsByName("GhostStore"))
                .invoke(exists -> assertThat(exists).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashed() {
        return clean()
                .chain(() -> persist("Trash Test"))
                .chain(id -> merchantCommandRepo.trashed(id))
                .invoke(m -> {
                    assertThat(m).isNotNull();
                    assertThat(m.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashed_ReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persist("Double Trash"))
                .chain(id -> merchantCommandRepo.trashed(id)
                        .chain(() -> merchantCommandRepo.trashed(id)))
                .invoke(m -> assertThat(m).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashed_ReturnsNullIfNotFound() {
        return clean()
                .chain(() -> merchantCommandRepo.trashed(99999L))
                .invoke(m -> assertThat(m).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestore() {
        return clean()
                .chain(() -> persist("Restore Me"))
                .chain(id -> merchantCommandRepo.trashed(id)
                        .chain(() -> merchantCommandRepo.restore(id)))
                .invoke(m -> {
                    assertThat(m).isNotNull();
                    assertThat(m.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestore_ReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("Active Only"))
                .chain(id -> merchantCommandRepo.restore(id))
                .invoke(m -> assertThat(m).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestore_ReturnsNullIfNotFound() {
        return clean()
                .chain(() -> merchantCommandRepo.restore(99999L))
                .invoke(m -> assertThat(m).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("Delete Perm"))
                .chain(id -> merchantCommandRepo.deletePermanent(id))
                .invoke(m -> assertThat(m).isNotNull())
                .chain(() -> merchantQueryRepo.findMerchantById(idCounter - 1)) 
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent_ReturnsNullIfNotFound() {
        return clean()
                .chain(() -> merchantCommandRepo.deletePermanent(99999L))
                .invoke(m -> assertThat(m).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("A").chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("B").chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> merchantCommandRepo.restoreAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> merchantQueryRepo.findActiveMerchants(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("Trash1").chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Trash2").chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Active1")) // tetap aktif
                .chain(() -> merchantCommandRepo.deleteAllDeleted())
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> merchantQueryRepo.findActiveMerchants(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus_Valid() {
        return clean()
                .chain(() -> persist("Change Status"))
                .chain(id -> merchantCommandRepo.updateStatus(id, "PENDING"))
                .invoke(result -> assertThat(result).isTrue())
                .chain(() -> merchantQueryRepo.findMerchantById(idCounter - 1))
                .invoke(m -> assertThat(m.getStatus()).isEqualTo(Status.PENDING))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus_InvalidStatus() {
        return clean()
                .chain(() -> persist("Invalid Status"))
                .chain(id -> merchantCommandRepo.updateStatus(id, "FAILED"))
                .invoke(result -> assertThat(result).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus_MerchantNotFound() {
        return clean()
                .chain(() -> merchantCommandRepo.updateStatus(99999L, "ACTIVE"))
                .invoke(result -> assertThat(result).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveMerchants() {
        return clean()
                .chain(() -> persist("Active1"))
                .chain(() -> persist("Active2"))
                .chain(() -> merchantQueryRepo.findActiveMerchants(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedMerchants() {
        return clean()
                .chain(() -> persist("ToTrash").chain(id -> merchantCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("KeepActive"))
                .chain(() -> merchantQueryRepo.findTrashedMerchants(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantsWithSearch() {
        return clean()
                .chain(() -> persist("Apple Store", "Sells gadgets", "123 Main St", "apple@mail.com", "1111", Status.SUCCESS))
                .chain(() -> persist("Banana Shop", "Food market", "456 Oak Ave", "banana@mail.com", "2222", Status.SUCCESS))
                .chain(() -> persist("Tech Corner", "Gadget repair", "789 Pine Rd", "tech@mail.com", "3333", Status.SUCCESS))
                .chain(() -> merchantQueryRepo.findMerchants(findAllReq(1, 10, "gadget")))
                .invoke(page -> {
                    assertThat(page.getTotalRecords()).isEqualTo(2);
                    assertThat(page.getData()).extracting(Merchant::getName)
                            .containsExactlyInAnyOrder("Apple Store", "Tech Corner");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantsWithPagination() {
        return clean()
                .chain(() -> persist("M1"))
                .chain(() -> persist("M2"))
                .chain(() -> persist("M3"))
                .chain(() -> persist("M4"))
                .chain(() -> persist("M5"))
                .chain(() -> merchantQueryRepo.findMerchants(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(() -> merchantQueryRepo.findMerchants(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByName() {
        return clean()
                .chain(() -> persist("Exact Match"))
                .chain(() -> merchantQueryRepo.findByName("Exact Match"))
                .invoke(m -> {
                    assertThat(m).isNotNull();
                    assertThat(m.getName()).isEqualTo("Exact Match");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByName_NotFound() {
        return clean()
                .chain(() -> merchantQueryRepo.findByName("No Such Name"))
                .invoke(m -> assertThat(m).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByUserId() {
        return clean()
                .chain(() -> {
                    Merchant m = new Merchant();
                    m.setMerchantId(idCounter++);
                    m.setName("User Merchant");
                    m.setUserId(10);
                    m.setStatus(Status.SUCCESS);
                    return merchantQueryRepo.persist(m);
                })
                .chain(() -> merchantQueryRepo.findByUserId(10))
                .invoke(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0).getName()).isEqualTo("User Merchant");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByUserId_Null() {
        return clean()
                .chain(() -> merchantQueryRepo.findByUserId(null))
                .invoke(list -> assertThat(list).isEmpty())
                .replaceWithVoid();
    }
}