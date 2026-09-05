package com.sanedge.merchant_award.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;

import org.junit.jupiter.api.Test;

import com.sanedge.merchant_award.domain.requests.FindAllMerchantRequest;
import com.sanedge.merchant_award.entity.MerchantCertificationAndAward;

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
class MerchantAwardRepositoryTest {

    @Inject
    MerchantAwardQueryRepository awardQueryRepo;

    @Inject
    MerchantAwardCommandRepository awardCommandRepo;

    private Uni<Long> persist(String title, String description, String issuedBy, String certificateUrl) {
        MerchantCertificationAndAward award = new MerchantCertificationAndAward();
        award.setTitle(title);
        award.setDescription(description);
        award.setIssuedBy(issuedBy);
        award.setCertificateUrl(certificateUrl);
        award.setIssueDate(Date.valueOf("2023-01-01"));
        award.setExpiryDate(Date.valueOf("2025-01-01"));
        award.setMerchantId(1); 
        return awardQueryRepo.persist(award).map(a -> a.id);
    }

    private Uni<Long> persist(String title) {
        return persist(title, "Description " + title, "Issuer " + title, "http://cert/" + title);
    }

    private Uni<Void> clean() {
        return awardQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllMerchantRequest findAllReq(int page, int size, String search) {
        FindAllMerchantRequest r = new FindAllMerchantRequest();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist("ISO 9001"))
                .chain(id -> awardQueryRepo.findById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getTitle()).isEqualTo("ISO 9001");
                    assertThat(found.getIssuedBy()).isEqualTo("Issuer ISO 9001");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantAwardById() {
        return clean()
                .chain(() -> persist("Safety Award"))
                .chain(id -> awardQueryRepo.findMerchantAwardById(id))
                .invoke(award -> {
                    assertThat(award).isNotNull();
                    assertThat(award.getTitle()).isEqualTo("Safety Award");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantAwardById_ReturnsNullIfNotFound() {
        return clean()
                .chain(() -> awardQueryRepo.findMerchantAwardById(999999L))
                .invoke(opt -> assertThat(opt).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantAwardById_ReturnsNullIfTrashed() {
        return clean()
                .chain(() -> persist("Old Award"))
                .chain(id -> awardCommandRepo.trashed(id)
                        .chain(() -> awardQueryRepo.findMerchantAwardById(id)))
                .invoke(opt -> assertThat(opt).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> awardQueryRepo.findById(999999L))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashed() {
        return clean()
                .chain(() -> persist("Trash Me"))
                .chain(id -> awardCommandRepo.trashed(id))
                .invoke(award -> {
                    assertThat(award).isNotNull();
                    assertThat(award.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashed_ReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persist("Already Trashed"))
                .chain(id -> awardCommandRepo.trashed(id)
                        .chain(() -> awardCommandRepo.trashed(id)))
                .invoke(a -> assertThat(a).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashed_ReturnsNullIfNotFound() {
        return clean()
                .chain(() -> awardCommandRepo.trashed(99999L))
                .invoke(a -> assertThat(a).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestore() {
        return clean()
                .chain(() -> persist("Restore Award"))
                .chain(id -> awardCommandRepo.trashed(id)
                        .chain(() -> awardCommandRepo.restore(id)))
                .invoke(award -> {
                    assertThat(award).isNotNull();
                    assertThat(award.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestore_ReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("Active Award"))
                .chain(id -> awardCommandRepo.restore(id))
                .invoke(a -> assertThat(a).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestore_ReturnsNullIfNotFound() {
        return clean()
                .chain(() -> awardCommandRepo.restore(99999L))
                .invoke(a -> assertThat(a).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("Delete Perm"))
                .chain(id -> awardCommandRepo.trashed(id)
                        .chain(() -> awardCommandRepo.deletePermanent(id))
                        .chain(deleted -> awardQueryRepo.findById(id)))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent_ReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("No Trash"))
                .chain(id -> awardCommandRepo.deletePermanent(id))
                .invoke(d -> assertThat(d).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("A").chain(id -> awardCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("B").chain(id -> awardCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> awardCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> awardQueryRepo.findActiveMerchantAwards(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("T1").chain(id -> awardCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("T2").chain(id -> awardCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Keep"))
                .chain(() -> awardCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> awardQueryRepo.findActiveMerchantAwards(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveMerchantAwards() {
        return clean()
                .chain(() -> persist("Active1"))
                .chain(() -> persist("Active2"))
                .chain(() -> awardQueryRepo.findActiveMerchantAwards(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedMerchantAwards() {
        return clean()
                .chain(() -> persist("T").chain(id -> awardCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("Keep"))
                .chain(() -> awardQueryRepo.findTrashedMerchantAwards(findAllReq(1, 10, "")))
                .invoke(page -> assertThat(page.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantAwardsWithSearch() {
        return clean()
                .chain(() -> persist("GoGreen", "Eco certification", "EcoOrg", "http://eco"))
                .chain(() -> persist("SafeWork", "Safety standard", "SafetyCo", "http://safe"))
                .chain(() -> persist("TechExcellence", "Tech innovation", "TechOrg", "http://tech"))
                .chain(() -> awardQueryRepo.findMerchantAwards(findAllReq(1, 10, "safety")))
                .invoke(page -> {
                    assertThat(page.getTotalRecords()).isEqualTo(1);
                    assertThat(page.getData().get(0).getTitle()).isEqualTo("SafeWork");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindMerchantAwardsWithPagination() {
        return clean()
                .chain(() -> persist("A"))
                .chain(() -> persist("B"))
                .chain(() -> persist("C"))
                .chain(() -> persist("D"))
                .chain(() -> persist("E"))
                .chain(() -> awardQueryRepo.findMerchantAwards(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(() -> awardQueryRepo.findMerchantAwards(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .replaceWithVoid();
    }
}