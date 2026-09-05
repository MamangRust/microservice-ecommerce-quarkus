package com.sanedge.merchant.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.CreateMerchantRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantRequest;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.repository.MerchantCommandRepository;
import com.sanedge.merchant.repository.MerchantQueryRepository;
import com.sanedge.merchant.service.MerchantCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import pb.user.UserQueryService;

@ApplicationScoped
public class MerchantCommandServiceImpl implements MerchantCommandService {
        private static final Logger logger = LoggerFactory.getLogger(MerchantCommandServiceImpl.class);

        private final UserQueryService userQueryService;
        private final MerchantQueryRepository merchantQueryRepository;
        private final MerchantCommandRepository merchantCommandRepository;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @Inject
        public MerchantCommandServiceImpl(
                        @GrpcClient("user") UserQueryService userQueryService,
                        MerchantQueryRepository merchantQueryRepository,
                        MerchantCommandRepository merchantCommandRepository,
                        RedisService redisService,
                        TracingMetrics tracingMetrics) {
                this.userQueryService = userQueryService;
                this.merchantQueryRepository = merchantQueryRepository;
                this.merchantCommandRepository = merchantCommandRepository;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponse>> createMerchant(CreateMerchantRequest req) {
                Attributes attrs = Attributes.builder()
                                .put("merchant.name", req.getName())
                                .put("user.id", req.getUserId())
                                .build();

                logger.info("Creating merchant | Name: {}, UserId: {}", req.getName(), req.getUserId());

                return tracingMetrics.traceAndMeasure("createMerchant", "create_merchant", attrs,
                                () -> userQueryService.findById(pb.user.UserCommon.FindByIdUserRequest.newBuilder()
                                                .setId(req.getUserId().intValue()).build())
                                                .chain(response -> {
                                                        if (response == null || !response.hasData()) {
                                                                logger.error("User not found with id {}",
                                                                                req.getUserId());
                                                                throw new ResourceNotFoundException("User not found");
                                                        }
                                                        return merchantQueryRepository.existsByName(req.getName());
                                                })
                                                .chain(nameExists -> {
                                                        if (nameExists) {
                                                                logger.error("Merchant name already taken | Name: {}",
                                                                                req.getName());
                                                                throw new ResourceAlreadyExistsException(
                                                                                "Merchant name already taken");
                                                        }

                                                        Merchant merchant = new Merchant();
                                                        merchant.setName(req.getName());
                                                        merchant.setUserId(req.getUserId().intValue());
                                                        merchant.setDescription(req.getDescription());
                                                        merchant.setAddress(req.getAddress());
                                                        merchant.setContactEmail(req.getContactEmail());
                                                        merchant.setContactPhone(req.getContactPhone());
                                                        merchant.setStatus(Status.valueOf(req.getStatus().toUpperCase()));

                                                        return merchantCommandRepository.persist(merchant)
                                                                        .chain(savedMerchant -> {
                                                                                logger.info("Merchant created successfully | Id: {}",
                                                                                                merchant.getMerchantId());
                                                                                return Uni.createFrom().item(ApiResponse
                                                                                                .success("Merchant created successfully",
                                                                                                                MerchantResponse.from(
                                                                                                                                merchant)));
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponse>> updateMerchant(UpdateMerchantRequest req) {
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", req.getMerchantId())
                                .build();

                logger.info("Updating merchant | Id: {}", req.getMerchantId());

                return tracingMetrics.traceAndMeasure("updateMerchant", "update_merchant", attrs,
                                () -> merchantQueryRepository.findMerchantById(req.getMerchantId().longValue())
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}",
                                                                                req.getMerchantId());
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        Uni<Void> userCheckUni = Uni.createFrom().nullItem();
                                                        if (req.getUserId() != null) {
                                                                userCheckUni = userQueryService
                                                                                .findById(pb.user.UserCommon.FindByIdUserRequest
                                                                                                .newBuilder()
                                                                                                .setId(req.getUserId()
                                                                                                                .intValue())
                                                                                                .build())
                                                                                .chain(response -> {
                                                                                        if (response == null
                                                                                                        || !response.hasData()) {
                                                                                                logger.error("User not found with id {}",
                                                                                                                req.getUserId());
                                                                                                throw new ResourceNotFoundException(
                                                                                                                "User not found");
                                                                                        }
                                                                                        merchant.setUserId(req
                                                                                                        .getUserId()
                                                                                                        .intValue());
                                                                                        return Uni.createFrom()
                                                                                                        .nullItem();
                                                                                });
                                                        }

                                                        return userCheckUni.chain(v -> {
                                                                merchant.setName(req.getName());
                                                                merchant.setDescription(req.getDescription());
                                                                merchant.setAddress(req.getAddress());
                                                                merchant.setContactEmail(req.getContactEmail());
                                                                merchant.setContactPhone(req.getContactPhone());
                                                                merchant.setStatus(Status.valueOf(
                                                                                req.getStatus().toUpperCase()));

                                                                return merchantCommandRepository.persist(merchant)
                                                                                .chain(savedMerchant -> {
                                                                                        String cacheIdKey = "merchant:id:"
                                                                                                        + req.getMerchantId();
                                                                                        String cacheUserKey = "merchant:user:"
                                                                                                        + merchant.getUserId();

                                                                                        return Uni.combine().all().unis(
                                                                                                        redisService.deleteReactive(
                                                                                                                        cacheIdKey),
                                                                                                        redisService.deleteReactive(
                                                                                                                        cacheUserKey))
                                                                                                        .asTuple()
                                                                                                        .map(v2 -> {
                                                                                                                logger.info("Merchant updated successfully | Id: {}",
                                                                                                                                req.getMerchantId());
                                                                                                                return ApiResponse
                                                                                                                                .success("Merchant updated successfully",
                                                                                                                                                MerchantResponse.from(
                                                                                                                                                                merchant));
                                                                                                        });
                                                                                });
                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponseDeleteAt>> trashMerchant(Long id) {
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", id)
                                .build();

                logger.info("Trashing merchant id={}", id);

                return tracingMetrics.traceAndMeasure("trashMerchant", "trash_merchant", attrs,
                                () -> merchantCommandRepository.trashed(id)
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}", id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        String cacheIdKey = "merchant:id:" + id;
                                                        String cacheUserKey = "merchant:user:" + merchant.getUserId();

                                                        return Uni.combine().all().unis(
                                                                        redisService.deleteReactive(cacheIdKey),
                                                                        redisService.deleteReactive(cacheUserKey))
                                                                        .asTuple().map(v2 -> {
                                                                                logger.info("Merchant trashed successfully | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Merchant trashed successfully",
                                                                                                MerchantResponseDeleteAt
                                                                                                                .from(merchant));
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<MerchantResponseDeleteAt>> restoreMerchant(Long id) {
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", id)
                                .build();

                logger.info("Restoring merchant id={}", id);

                return tracingMetrics.traceAndMeasure("restoreMerchant", "restore_merchant", attrs,
                                () -> merchantCommandRepository.restore(id)
                                                .chain(merchant -> {
                                                        if (merchant == null) {
                                                                logger.error("Merchant not found with id {}", id);
                                                                throw new ResourceNotFoundException(
                                                                                "Merchant not found");
                                                        }

                                                        String cacheIdKey = "merchant:id:" + id;
                                                        String cacheUserKey = "merchant:user:" + merchant.getUserId();

                                                        return Uni.combine().all().unis(
                                                                        redisService.deleteReactive(cacheIdKey),
                                                                        redisService.deleteReactive(cacheUserKey))
                                                                        .asTuple().map(v2 -> {
                                                                                logger.info("Merchant restored successfully | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.success(
                                                                                                "Merchant restored successfully",
                                                                                                MerchantResponseDeleteAt
                                                                                                                .from(merchant));
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteMerchant(Long id) {
                Attributes attrs = Attributes.builder()
                                .put("merchant.id", id)
                                .build();

                logger.info("Permanently deleting merchant id={}", id);

                return tracingMetrics.traceAndMeasure("deleteMerchant", "delete_merchant", attrs,
                                () -> merchantCommandRepository.deletePermanent(id)
                                                .chain(deletedMerchant -> {
                                                        if (deletedMerchant == null) {
                                                                logger.error("Merchant permanent delete failed - not found or must be trashed with id {}",
                                                                                id);
                                                                throw new InvalidRequestException(
                                                                                "Merchant not found or must be trashed before permanent deletion");
                                                        }

                                                        String cacheIdKey = "merchant:id:" + id;

                                                        return redisService.deleteReactive(cacheIdKey)
                                                                        .map(v2 -> {
                                                                                logger.info("Merchant permanently deleted | Id: {}",
                                                                                                id);
                                                                                return ApiResponse.<Void>success(
                                                                                                "Merchant permanently deleted");
                                                                        });
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> restoreAll() {
                logger.info("Restoring ALL trashed merchants");

                return tracingMetrics.traceAndMeasure("restoreAllMerchants", "restore_all_merchants",
                                () -> merchantCommandRepository.restoreAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException(
                                                                                "No trashed merchants found");
                                                        }
                                                        logger.info("Restored all trashed merchants");
                                                        return ApiResponse.<Void>success(
                                                                        "Restored all trashed merchants");
                                                }));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteAll() {
                logger.info("Permanently deleting ALL trashed merchants");

                return tracingMetrics.traceAndMeasure("deleteAllMerchants", "delete_all_merchants",
                                () -> merchantCommandRepository.deleteAllDeleted()
                                                .map(success -> {
                                                        if (!success) {
                                                                throw new ResourceNotFoundException(
                                                                                "No trashed merchants found");
                                                        }
                                                        logger.info("Deleted all trashed merchants");
                                                        return ApiResponse
                                                                        .<Void>success("Deleted all trashed merchants");
                                                }));
        }
}