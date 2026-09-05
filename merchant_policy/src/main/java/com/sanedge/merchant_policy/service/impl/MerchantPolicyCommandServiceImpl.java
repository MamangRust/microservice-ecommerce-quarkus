package com.sanedge.merchant_policy.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant_policy.domain.requests.CreateMerchantPolicyRequest;
import com.sanedge.merchant_policy.domain.requests.UpdateMerchantPolicyRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponse;
import com.sanedge.merchant_policy.domain.response.MerchantPoliciesResponseDeleteAt;
import com.sanedge.merchant_policy.entity.MerchantPolicy;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.merchant_policy.repository.MerchantPolicyCommandRepository;
import com.sanedge.merchant_policy.service.MerchantPolicyCommandService;

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
public class MerchantPolicyCommandServiceImpl implements MerchantPolicyCommandService {
    private static final Logger logger = LoggerFactory.getLogger(MerchantPolicyCommandServiceImpl.class);

    private final MerchantPolicyCommandRepository merchantPolicyCommandRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final MerchantQueryService merchantQueryService;

    @Inject
    public MerchantPolicyCommandServiceImpl(
            MerchantPolicyCommandRepository merchantPolicyCommandRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics,
            @GrpcClient("merchant") MerchantQueryService merchantQueryService) {
        this.merchantPolicyCommandRepository = merchantPolicyCommandRepository;
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

    private Uni<Void> invalidateCache(Long policyId) {
        if (policyId != null) {
            return redisService.deleteReactive("merchantpolicy:id:" + policyId);
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantPoliciesResponse>> create(CreateMerchantPolicyRequest request) {
        logger.info("Creating merchant policy for merchantId={} title={}", request.getMerchantId(), request.getTitle());

        try {
            validateRequest(request);
        } catch (Exception e) {
            logger.error("Validation failed for create merchant policy", e);
            return Uni.createFrom().failure(e);
        }

        return tracingMetrics.traceAndMeasure("createMerchantPolicy", "create_policy",
                Attributes.builder().put("merchant.id", request.getMerchantId().toString()).build(),
                () -> merchantQueryService.findById(
                        pb.merchant.MerchantCommon.FindByIdMerchantRequest.newBuilder()
                                .setId(request.getMerchantId())
                                .build())
                        .chain(merchantResponse -> {
                            if (merchantResponse == null || !merchantResponse.hasData()
                                    || merchantResponse.getData().getId() == 0) {
                                logger.warn("Merchant not found with id {}", request.getMerchantId());
                                throw new ResourceNotFoundException(
                                        "Merchant not found with id " + request.getMerchantId());
                            }

                            MerchantPolicy policy = new MerchantPolicy();
                            policy.setMerchantId(request.getMerchantId());
                            policy.setPolicyType(request.getPolicyType());
                            policy.setTitle(request.getTitle());
                            policy.setDescription(request.getDescription());
                            policy.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                            policy.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                            return merchantPolicyCommandRepository.persist(policy)
                                    .chain(saved -> {
                                        MerchantPoliciesResponse response = MerchantPoliciesResponse.from(saved);

                                        return invalidateCache(saved.id)
                                                .map(v -> {
                                                    logger.info("Successfully created merchant policy with ID: {}",
                                                            saved.id);
                                                    return ApiResponse.success("Merchant policy created successfully!",
                                                            response);
                                                });
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to create merchant policy for merchant ID: {}",
                                request.getMerchantId(), e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantPoliciesResponse>> update(UpdateMerchantPolicyRequest request) {
        logger.info("Updating merchant policy id={}", request.getMerchantPolicyId());

        try {
            validateRequest(request);
        } catch (Exception e) {
            logger.error("Validation failed for update merchant policy", e);
            return Uni.createFrom().failure(e);
        }

        if (request.getMerchantPolicyId() == null) {
            logger.error("MerchantPolicyId is required for update");
            return Uni.createFrom().failure(new ResourceNotFoundException("MerchantPolicyId is required"));
        }

        return tracingMetrics.traceAndMeasure("updateMerchantPolicy", "update_policy",
                Attributes.builder().put("policy.id", request.getMerchantPolicyId().toString()).build(),
                () -> merchantPolicyCommandRepository.findById(request.getMerchantPolicyId().longValue())
                        .chain(policy -> {
                            if (policy == null) {
                                logger.warn("Merchant policy not found: {}", request.getMerchantPolicyId());
                                throw new ResourceNotFoundException(
                                        "Merchant policy not found with id " + request.getMerchantPolicyId());
                            }

                            policy.setPolicyType(request.getPolicyType());
                            policy.setTitle(request.getTitle());
                            policy.setDescription(request.getDescription());
                            policy.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

                            return merchantPolicyCommandRepository.persist(policy)
                                    .chain(saved -> {
                                        MerchantPoliciesResponse response = MerchantPoliciesResponse.from(saved);

                                        return invalidateCache(saved.id)
                                                .map(v -> {
                                                    logger.info("Successfully updated merchant policy with ID: {}",
                                                            saved.id);
                                                    return ApiResponse.success("Merchant policy updated successfully!",
                                                            response);
                                                });
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to update merchant policy ID: {}",
                                request.getMerchantPolicyId(), e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantPoliciesResponseDeleteAt>> trash(Long id) {
        logger.info("Trashing merchant policy id={}", id);

        return tracingMetrics.traceAndMeasure("trashMerchantPolicy", "trash_policy",
                Attributes.builder().put("policy.id", id.toString()).build(),
                () -> merchantPolicyCommandRepository.trash(id)
                        .chain(policy -> {
                            if (policy == null) {
                                logger.warn(
                                        "Failed to trash merchant policy - not found or already trashed with ID: {}",
                                        id);
                                throw new ResourceNotFoundException("Merchant policy not found or already trashed");
                            }

                            MerchantPoliciesResponseDeleteAt response = MerchantPoliciesResponseDeleteAt.from(policy);

                            return invalidateCache(id)
                                    .map(v -> {
                                        logger.info("Successfully trashed merchant policy with ID: {}", id);
                                        return ApiResponse.success("Merchant policy trashed successfully!", response);
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to trash merchant policy ID: {}", id, e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<MerchantPoliciesResponseDeleteAt>> restore(Long id) {
        logger.info("Restoring merchant policy id={}", id);

        return tracingMetrics.traceAndMeasure("restoreMerchantPolicy", "restore_policy",
                Attributes.builder().put("policy.id", id.toString()).build(),
                () -> merchantPolicyCommandRepository.restore(id)
                        .chain(policy -> {
                            if (policy == null) {
                                logger.warn("Failed to restore merchant policy - not found or not trashed with ID: {}",
                                        id);
                                throw new ResourceNotFoundException("Merchant policy not found or not trashed");
                            }

                            MerchantPoliciesResponseDeleteAt response = MerchantPoliciesResponseDeleteAt.from(policy);

                            return invalidateCache(id)
                                    .map(v -> {
                                        logger.info("Successfully restored merchant policy with ID: {}", id);
                                        return ApiResponse.success("Merchant policy restored successfully!", response);
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to restore merchant policy ID: {}", id, e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> delete(Long id) {
        Attributes attrs = Attributes.builder().put("policy.id", id.toString()).build();
        logger.warn("Permanently deleting merchant policy id={}", id);

        return tracingMetrics.traceAndMeasure("deleteMerchantPolicy", "delete_policy_permanent", attrs, () -> {
            return merchantPolicyCommandRepository.deletePermanent(id)
                    .chain(deletedPolicy -> {
                        if (deletedPolicy == null) {
                            logger.warn("Permanent delete failed - merchant policy not found or must be trashed before permanent deletion with id: {}", id);
                            throw new InvalidRequestException("Merchant policy not found or must be trashed before permanent deletion");
                        }

                        return invalidateCache(id)
                                .map(v2 -> {
                                    logger.info("Successfully permanently deleted merchant policy with ID: {}", id);
                                    return ApiResponse.success("Merchant policy permanently deleted!");
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAll() {
        logger.info("Restoring ALL trashed merchant policies");

        return tracingMetrics.traceAndMeasure("restoreAllMerchantPolicies", "restore_all_policies", () -> {
            return merchantPolicyCommandRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant policies found");
                        }
                        logger.info("Successfully restored all trashed merchant policies");
                        return ApiResponse.success("All merchant policies restored successfully!");
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAll() {
        logger.warn("Permanently deleting ALL trashed merchant policies");

        return tracingMetrics.traceAndMeasure("deleteAllMerchantPolicies", "delete_all_policies_permanent", () -> {
            return merchantPolicyCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed merchant policies found");
                        }
                        logger.info("Successfully permanently deleted all trashed merchant policies");
                        return ApiResponse.success("All merchant policies permanently deleted!");
                    });
        });
    }
}