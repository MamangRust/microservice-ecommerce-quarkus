package com.sanedge.merchant_business.service.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.merchant_business.domain.requests.CreateMerchantBusinessRequest;
import com.sanedge.merchant_business.domain.requests.UpdateMerchantBusinessRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponse;
import com.sanedge.merchant_business.domain.response.MerchantBusinessResponseDeleteAt;
import com.sanedge.merchant_business.entity.MerchantBusinessInformation;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.merchant_business.repository.MerchantBusinessCommandRepository;
import com.sanedge.merchant_business.repository.MerchantBusinessQueryRepository;
import com.sanedge.merchant_business.service.MerchantBusinessCommandService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import pb.merchant.MerchantQueryService;

@ApplicationScoped
public class MerchantBusinessCommandServiceImpl implements MerchantBusinessCommandService {
    private static final Logger logger = LoggerFactory.getLogger(MerchantBusinessCommandServiceImpl.class);

    private final MerchantBusinessCommandRepository merchantBusinessCommandRepository;
    private final MerchantBusinessQueryRepository merchantBusinessQueryRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final MerchantQueryService merchantQueryService;

    @Inject
    public MerchantBusinessCommandServiceImpl(MerchantBusinessCommandRepository merchantBusinessCommandRepository,
            MerchantBusinessQueryRepository merchantBusinessQueryRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics,
            @GrpcClient("merchant") MerchantQueryService merchantQueryService) {
        this.merchantBusinessCommandRepository = merchantBusinessCommandRepository;
        this.merchantBusinessQueryRepository = merchantBusinessQueryRepository;
        this.validator = validator;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
        this.merchantQueryService = merchantQueryService;
    }

    private <T> void validateRequest(T req) {
        Set<ConstraintViolation<T>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            logger.error("Validation failed: {}", sb);
            throw new ConstraintViolationException("Validation failed: " + sb, violations);
        }
    }

    private Uni<Void> invalidateCache(Long businessId) {
        if (businessId != null) {
            return redisService.deleteReactive("merchantbusiness:id:" + businessId)
                    .replaceWith(Uni.createFrom().voidItem());
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantBusinessResponse>> createMerchantBusiness(CreateMerchantBusinessRequest req) {
        validateRequest(req);

        Attributes attrs = Attributes.builder()
                .put("merchant.id", req.getMerchantId())
                .build();

        logger.info("Creating merchant business info for merchant ID: {}", req.getMerchantId());

        return tracingMetrics.traceAndMeasure("createMerchantBusiness", "create_business", attrs,
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

                            MerchantBusinessInformation business = MerchantBusinessInformation.fromCreateRequest(req);
                            return merchantBusinessCommandRepository.persist(business)
                                    .chain(saved -> {
                                        MerchantBusinessResponse response = MerchantBusinessResponse.from(saved);

                                        return invalidateCache(saved.id)
                                                .map(v -> {
                                                    logger.info(
                                                            "Successfully created merchant business info with id: {}",
                                                            saved.id);
                                                    return ApiResponse.success(
                                                            "Merchant business info created successfully", response);
                                                });
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantBusinessResponse>> updateMerchantBusiness(UpdateMerchantBusinessRequest req) {
        validateRequest(req);

        if (req.getMerchantBusinessInfoId() == null) {
            throw new ResourceNotFoundException("MerchantBusinessInfoId is required");
        }

        Attributes attrs = Attributes.builder()
                .put("business.id", req.getMerchantBusinessInfoId())
                .build();

        logger.info("Updating merchant business info ID: {}", req.getMerchantBusinessInfoId());

        return tracingMetrics.traceAndMeasure("updateMerchantBusiness", "update_business", attrs,
                () -> merchantBusinessQueryRepository
                        .findMerchantBusinessInformationById(req.getMerchantBusinessInfoId().longValue())
                        .chain(business -> {
                            if (business == null) {
                                logger.warn("Merchant business info not found: {}", req.getMerchantBusinessInfoId());
                                throw new ResourceNotFoundException(
                                        "Merchant business info not found with id " + req.getMerchantBusinessInfoId());
                            }

                            business.setBusinessType(req.getBusinessType());
                            business.setTaxId(req.getTaxId());
                            business.setEstablishedYear(req.getEstablishedYear());
                            business.setNumberOfEmployees(req.getNumberOfEmployees());
                            business.setWebsiteUrl(req.getWebsiteUrl());

                            return merchantBusinessCommandRepository.persist(business)
                                    .chain(saved -> {
                                        MerchantBusinessResponse response = MerchantBusinessResponse.from(saved);

                                        return invalidateCache(saved.id)
                                                .map(v -> {
                                                    logger.info(
                                                            "Successfully updated merchant business info with id: {}",
                                                            saved.id);
                                                    return ApiResponse.success(
                                                            "Merchant business info updated successfully", response);
                                                });
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantBusinessResponseDeleteAt>> trashedMerchantBusiness(Long merchantBusinessInfoId) {
        Attributes attrs = Attributes.builder()
                .put("business.id", merchantBusinessInfoId)
                .build();

        logger.info("Soft deleting merchant business info ID: {}", merchantBusinessInfoId);

        return tracingMetrics.traceAndMeasure("trashedMerchantBusiness", "trash_business", attrs,
                () -> merchantBusinessCommandRepository.trashed(merchantBusinessInfoId)
                        .chain(business -> {
                            if (business == null) {
                                logger.warn("Failed to trash merchant business info ID: {}", merchantBusinessInfoId);
                                throw new ResourceNotFoundException(
                                        "Merchant business info not found or already trashed");
                            }

                            MerchantBusinessResponseDeleteAt response = MerchantBusinessResponseDeleteAt.from(business);

                            return invalidateCache(merchantBusinessInfoId)
                                    .map(v -> {
                                        logger.info("Successfully trashed merchant business info with id: {}",
                                                merchantBusinessInfoId);
                                        return ApiResponse.success("Merchant business info trashed successfully",
                                                response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantBusinessResponseDeleteAt>> restoreMerchantBusiness(Long merchantBusinessInfoId) {
        Attributes attrs = Attributes.builder()
                .put("business.id", merchantBusinessInfoId)
                .build();

        logger.info("Restoring merchant business info ID: {}", merchantBusinessInfoId);

        return tracingMetrics.traceAndMeasure("restoreMerchantBusiness", "restore_business", attrs,
                () -> merchantBusinessCommandRepository.restore(merchantBusinessInfoId)
                        .chain(business -> {
                            if (business == null) {
                                logger.warn("Failed to restore merchant business info ID: {}", merchantBusinessInfoId);
                                throw new ResourceNotFoundException("Merchant business info not found or not trashed");
                            }

                            MerchantBusinessResponseDeleteAt response = MerchantBusinessResponseDeleteAt.from(business);

                            return invalidateCache(merchantBusinessInfoId)
                                    .map(v -> {
                                        logger.info("Successfully restored merchant business info with id: {}",
                                                merchantBusinessInfoId);
                                        return ApiResponse.success("Merchant business info restored successfully",
                                                response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteMerchantBusinessPermanent(Long merchantBusinessInfoId) {
        Attributes attrs = Attributes.builder()
                .put("business.id", merchantBusinessInfoId)
                .build();

        logger.warn("Permanently deleting merchant business info ID: {}", merchantBusinessInfoId);

        return tracingMetrics.traceAndMeasure("deleteMerchantBusinessPermanent", "delete_business_permanent", attrs, () -> {
            return merchantBusinessCommandRepository.deletePermanent(merchantBusinessInfoId)
                    .chain(deletedBusiness -> {
                        if (deletedBusiness == null) {
                            logger.warn("Permanent delete failed - business info not found or must be trashed before permanent deletion with id: {}",
                                    merchantBusinessInfoId);
                            throw new InvalidRequestException(
                                    "Merchant business info not found or must be trashed before permanent deletion");
                        }

                        return invalidateCache(merchantBusinessInfoId)
                                .map(v -> {
                                    logger.info("Successfully permanently deleted merchant business info with id: {}",
                                            merchantBusinessInfoId);
                                    return ApiResponse.success("Merchant business info permanently deleted");
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAllMerchantBusiness() {
        logger.info("Restoring all trashed merchant business info");

        return tracingMetrics.traceAndMeasure("restoreAllMerchantBusiness", "restore_all_business_info", () -> {
            return merchantBusinessCommandRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant business info found");
                        }
                        logger.info("Successfully restored all trashed merchant business info");
                        return ApiResponse.success("All trashed merchant business info restored");
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAllMerchantBusinessPermanent() {
        logger.warn("Permanently deleting all trashed merchant business info");

        return tracingMetrics.traceAndMeasure("deleteAllMerchantBusinessPermanent",
                "delete_all_business_info_permanent", () -> {
            return merchantBusinessCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant business info found");
                        }
                        logger.info("Successfully permanently deleted all trashed merchant business info");
                        return ApiResponse.success("All trashed merchant business info permanently deleted");
                    });
        });
    }
}