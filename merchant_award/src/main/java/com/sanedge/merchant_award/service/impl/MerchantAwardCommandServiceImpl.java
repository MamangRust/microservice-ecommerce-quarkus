package com.sanedge.merchant_award.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.merchant_award.domain.requests.CreateMerchantAwardRequest;
import com.sanedge.merchant_award.domain.requests.UpdateMerchantAwardRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponse;
import com.sanedge.merchant_award.domain.response.MerchantAwardResponseDeleteAt;
import com.sanedge.merchant_award.entity.MerchantCertificationAndAward;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.merchant_award.repository.MerchantAwardCommandRepository;
import com.sanedge.merchant_award.service.MerchantAwardCommandService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pb.merchant.MerchantQueryService;

@ApplicationScoped
public class MerchantAwardCommandServiceImpl implements MerchantAwardCommandService {
    private static final Logger logger = LoggerFactory.getLogger(MerchantAwardCommandServiceImpl.class);

    private final MerchantAwardCommandRepository merchantAwardCommandRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final MerchantQueryService merchantQueryService;

    @Inject
    public MerchantAwardCommandServiceImpl(
            MerchantAwardCommandRepository merchantAwardCommandRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics,
            @GrpcClient("merchant") MerchantQueryService merchantQueryService) {
        this.merchantAwardCommandRepository = merchantAwardCommandRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
        this.merchantQueryService = merchantQueryService;
    }

    private Uni<Void> invalidateCache(Long awardId) {
        if (awardId != null) {
            return redisService.deleteReactive("merchantawards:id:" + awardId).replaceWith(Uni.createFrom().voidItem());
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantAwardResponse>> createMerchantAward(CreateMerchantAwardRequest req) {
        Attributes attrs = Attributes.builder()
                .put("award.title", req.getTitle())
                .build();

        logger.info("Creating merchant award: {}", req.getTitle());

        return tracingMetrics.traceAndMeasure("createMerchantAward", "create_award", attrs,
                () -> merchantQueryService.findById(
                        pb.merchant.MerchantCommon.FindByIdMerchantRequest.newBuilder()
                                .setId(req.getMerchantId())
                                .build())
                        .chain(merchantResponse -> {
                            if (merchantResponse == null || !merchantResponse.hasData()
                                    || merchantResponse.getData().getId() == 0) {
                                logger.warn("Merchant not found with id {}", req.getMerchantId());
                                throw new ResourceNotFoundException(
                                        "Merchant not found with id " + req.getMerchantId());
                            }

                            MerchantCertificationAndAward award = MerchantCertificationAndAward.fromCreateRequest(req);
                            return merchantAwardCommandRepository.persist(award)
                                    .chain(saved -> {
                                        MerchantAwardResponse response = MerchantAwardResponse.from(saved);

                                        return invalidateCache(saved.id)
                                                .map(v -> {
                                                    logger.info("Successfully created merchant award with id: {}",
                                                            saved.id);
                                                    return ApiResponse.success("Merchant award created successfully",
                                                            response);
                                                });
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantAwardResponse>> updateMerchantAward(UpdateMerchantAwardRequest req) {
        if (req.getMerchantCertificationId() == null) {
            throw new ResourceNotFoundException("MerchantCertificationId is required");
        }

        Attributes attrs = Attributes.builder()
                .put("award.id", req.getMerchantCertificationId())
                .build();

        logger.info("Updating merchant award ID: {}", req.getMerchantCertificationId());

        return tracingMetrics.traceAndMeasure("updateMerchantAward", "update_award", attrs,
                () -> merchantAwardCommandRepository.findById(req.getMerchantCertificationId().longValue())
                        .chain(award -> {
                            if (award == null) {
                                logger.warn("Merchant award not found: {}", req.getMerchantCertificationId());
                                throw new ResourceNotFoundException(
                                        "Merchant award not found with id " + req.getMerchantCertificationId());
                            }

                            award.updateFromRequest(req);
                            return merchantAwardCommandRepository.persist(award)
                                    .chain(saved -> {
                                        MerchantAwardResponse response = MerchantAwardResponse.from(saved);

                                        return invalidateCache(saved.id)
                                                .map(v -> {
                                                    logger.info("Successfully updated merchant award with id: {}",
                                                            saved.id);
                                                    return ApiResponse.success("Merchant award updated successfully",
                                                            response);
                                                });
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantAwardResponseDeleteAt>> trashedMerchantAward(Long merchantAwardId) {
        Attributes attrs = Attributes.builder()
                .put("award.id", merchantAwardId)
                .build();

        logger.info("Soft deleting merchant award ID: {}", merchantAwardId);

        return tracingMetrics.traceAndMeasure("trashedMerchantAward", "trash_award", attrs,
                () -> merchantAwardCommandRepository.trashed(merchantAwardId)
                        .chain(award -> {
                            if (award == null) {
                                logger.warn("Failed to trash merchant award ID: {}", merchantAwardId);
                                throw new ResourceNotFoundException("Merchant award not found or already trashed");
                            }

                            MerchantAwardResponseDeleteAt response = MerchantAwardResponseDeleteAt.from(award);

                            return invalidateCache(merchantAwardId)
                                    .map(v -> {
                                        logger.info("Successfully trashed merchant award with id: {}", merchantAwardId);
                                        return ApiResponse.success("Merchant award trashed successfully", response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantAwardResponseDeleteAt>> restoreMerchantAward(Long merchantAwardId) {
        Attributes attrs = Attributes.builder()
                .put("award.id", merchantAwardId)
                .build();

        logger.info("Restoring merchant award ID: {}", merchantAwardId);

        return tracingMetrics.traceAndMeasure("restoreMerchantAward", "restore_award", attrs,
                () -> merchantAwardCommandRepository.restore(merchantAwardId)
                        .chain(award -> {
                            if (award == null) {
                                logger.warn("Failed to restore merchant award ID: {}", merchantAwardId);
                                throw new ResourceNotFoundException("Merchant award not found or not trashed");
                            }

                            MerchantAwardResponseDeleteAt response = MerchantAwardResponseDeleteAt.from(award);

                            return invalidateCache(merchantAwardId)
                                    .map(v -> {
                                        logger.info("Successfully restored merchant award with id: {}",
                                                merchantAwardId);
                                        return ApiResponse.success("Merchant award restored successfully", response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteMerchantAwardPermanent(Long merchantAwardId) {
        Attributes attrs = Attributes.builder()
                .put("award.id", merchantAwardId)
                .build();

        logger.warn("Permanently deleting merchant award ID: {}", merchantAwardId);

        return tracingMetrics.traceAndMeasure("deleteMerchantAwardPermanent", "delete_award_permanent", attrs, () -> {
            return merchantAwardCommandRepository.deletePermanent(merchantAwardId)
                    .chain(deletedAward -> {
                        if (deletedAward == null) {
                            logger.warn("Permanent delete failed - award not found or must be trashed before permanent deletion with id: {}",
                                    merchantAwardId);
                            throw new InvalidRequestException(
                                    "Merchant award not found or must be trashed before permanent deletion");
                        }

                        return invalidateCache(merchantAwardId)
                                .map(v -> {
                                    logger.info("Successfully permanently deleted merchant award with id: {}",
                                            merchantAwardId);
                                    return ApiResponse.success("Merchant award permanently deleted");
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAllMerchantAward() {
        logger.info("Restoring all trashed merchant awards");

        return tracingMetrics.traceAndMeasure("restoreAllMerchantAward", "restore_all_awards", () -> {
            return merchantAwardCommandRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant awards found");
                        }
                        logger.info("Successfully restored all trashed merchant awards");
                        return ApiResponse.success("All trashed merchant awards restored");
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAllMerchantAwardPermanent() {
        logger.warn("Permanently deleting all trashed merchant awards");

        return tracingMetrics.traceAndMeasure("deleteAllMerchantAwardPermanent", "delete_all_awards_permanent", () -> {
            return merchantAwardCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant awards found");
                        }
                        logger.info("Successfully permanently deleted all trashed merchant awards");
                        return ApiResponse.success("All trashed merchant awards permanently deleted");
                    });
        });
    }
}