package com.sanedge.banner.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.banner.domain.requests.CreateBannerRequest;
import com.sanedge.banner.domain.requests.UpdateBannerRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.banner.domain.response.BannerResponse;
import com.sanedge.banner.domain.response.BannerResponseDeleteAt;
import com.sanedge.banner.entity.Banner;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.banner.repository.BannerCommandRepository;
import com.sanedge.banner.repository.BannerQueryRepository;
import com.sanedge.banner.service.BannerCommandService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BannerCommandServiceImpl implements BannerCommandService {
    private static final Logger logger = LoggerFactory.getLogger(BannerCommandServiceImpl.class);

    private final BannerQueryRepository bannerQueryRepository;
    private final BannerCommandRepository bannerCommandRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    public BannerCommandServiceImpl(BannerQueryRepository bannerQueryRepository,
            BannerCommandRepository bannerCommandRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.bannerQueryRepository = bannerQueryRepository;
        this.bannerCommandRepository = bannerCommandRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<BannerResponse>> createBanner(CreateBannerRequest request) {
        Attributes attrs = Attributes.builder()
                .put("banner.name", request.getName())
                .build();

        logger.info("Creating new banner with name: {}", request.getName());

        return tracingMetrics.traceAndMeasure("createBanner", "create_banner", attrs, () -> 
            bannerQueryRepository.findByName(request.getName())
                .chain(existingBanner -> {
                    if (existingBanner != null) {
                        logger.warn("Banner creation failed - banner name '{}' already exists", request.getName());
                        throw new ResourceAlreadyExistsException("Banner with name '" + request.getName() + "' already exists");
                    }

                    Banner banner = new Banner();
                    banner.setName(request.getName());
                    banner.setStartDate(java.sql.Date.valueOf(java.time.LocalDate.parse(request.getStartDate())));
                    banner.setEndDate(java.sql.Date.valueOf(java.time.LocalDate.parse(request.getEndDate())));
                    banner.setStartTime(java.sql.Time.valueOf(java.time.LocalTime.parse(request.getStartTime())));
                    banner.setEndTime(java.sql.Time.valueOf(java.time.LocalTime.parse(request.getEndTime())));
                    banner.setIsActive(request.getIsActive());
                    banner.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    banner.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                    return bannerCommandRepository.persist(banner)
                            .map(v -> {
                                BannerResponse bannerResponse = BannerResponse.from(banner);
                                logger.info("Successfully created banner with id: {} and name: {}", banner.id, banner.getName());
                                return ApiResponse.success("Banner created successfully!", bannerResponse);
                            });
                })
        );
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<BannerResponse>> updateBanner(UpdateBannerRequest request) {
        if (request.getId() == null) {
            throw new ResourceNotFoundException("banner_id is required");
        }

        Attributes attrs = Attributes.builder()
                .put("banner.id", request.getId())
                .build();

        logger.info("Updating banner with id: {}", request.getId());

        return tracingMetrics.traceAndMeasure("updateBanner", "update_banner", attrs, () -> 
            bannerCommandRepository.findById(request.getId())
                .chain(existingBanner -> {
                    if (existingBanner == null) {
                        logger.warn("Banner update failed - banner not found with id: {}", request.getId());
                        throw new ResourceNotFoundException("Banner not found");
                    }

                    existingBanner.setName(request.getName());
                    existingBanner.setStartDate(java.sql.Date.valueOf(java.time.LocalDate.parse(request.getStartDate())));
                    existingBanner.setEndDate(java.sql.Date.valueOf(java.time.LocalDate.parse(request.getEndDate())));
                    existingBanner.setStartTime(java.sql.Time.valueOf(java.time.LocalTime.parse(request.getStartTime())));
                    existingBanner.setEndTime(java.sql.Time.valueOf(java.time.LocalTime.parse(request.getEndTime())));
                    existingBanner.setIsActive(request.getIsActive());
                    existingBanner.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                    return bannerCommandRepository.persist(existingBanner)
                            .chain(v -> {
                                BannerResponse bannerResponse = BannerResponse.from(existingBanner);
                                String cacheKey = "banner:" + request.getId();

                                return redisService.deleteReactive(cacheKey)
                                        .map(v2 -> {
                                            logger.info("Invalidated cache for key: {}", cacheKey);
                                            logger.info("Successfully updated banner with id: {}", request.getId());
                                            return ApiResponse.success("Banner updated successfully!", bannerResponse);
                                        });
                            });
                })
        );
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<BannerResponseDeleteAt>> trashedBanner(Long bannerId) {
        Attributes attrs = Attributes.builder()
                .put("banner.id", bannerId)
                .build();

        logger.info("Trashing banner with id: {}", bannerId);

        return tracingMetrics.traceAndMeasure("trashBanner", "trash_banner", attrs, () -> 
            bannerCommandRepository.trash(bannerId)
                .chain(trashedBanner -> {
                    if (trashedBanner == null) {
                        logger.warn("Banner trash failed - banner not found with id: {}", bannerId);
                        throw new ResourceNotFoundException("Trashed banner not found with id: " + bannerId);
                    }

                    BannerResponseDeleteAt response = BannerResponseDeleteAt.from(trashedBanner);
                    String cacheKey = "banner:" + bannerId;

                    return redisService.deleteReactive(cacheKey)
                            .map(v -> {
                                logger.info("Invalidated cache for key: {}", cacheKey);
                                logger.info("Successfully trashed banner with id: {}", bannerId);
                                return ApiResponse.success("Banner trashed successfully!", response);
                            });
                })
        );
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<BannerResponseDeleteAt>> restoreBanner(Long bannerId) {
        Attributes attrs = Attributes.builder()
                .put("banner.id", bannerId)
                .build();

        logger.info("Restoring banner with id: {}", bannerId);

        return tracingMetrics.traceAndMeasure("restoreBanner", "restore_banner", attrs, () -> 
            bannerCommandRepository.restore(bannerId)
                .chain(restoredBanner -> {
                    if (restoredBanner == null) {
                        logger.warn("Banner restore failed - banner not found with id: {}", bannerId);
                        throw new ResourceNotFoundException("Restore banner not found with id: " + bannerId);
                    }

                    BannerResponseDeleteAt response = BannerResponseDeleteAt.from(restoredBanner);
                    String cacheKey = "banner:" + bannerId;

                    return redisService.deleteReactive(cacheKey)
                            .map(v -> {
                                logger.info("Invalidated cache for key: {}", cacheKey);
                                logger.info("Successfully restored banner with id: {}", bannerId);
                                return ApiResponse.success("Banner restored successfully!", response);
                            });
                })
        );
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteBannerPermanent(Long bannerId) {
        Attributes attrs = Attributes.builder()
                .put("banner.id", bannerId)
                .build();

        logger.info("Permanently deleting banner with id: {}", bannerId);

        return tracingMetrics.traceAndMeasure("deleteBannerPermanent", "delete_banner_permanent", attrs, () -> 
            bannerCommandRepository.deletePermanent(bannerId)
                .chain(deletedBanner -> {
                    if (deletedBanner == null) {
                        logger.warn("Permanent delete failed - banner not found or must be trashed before permanent deletion with id: {}", bannerId);
                        throw new InvalidRequestException("Banner not found or must be trashed before permanent deletion");
                    }

                    String cacheKey = "banner:" + bannerId;
                    return redisService.deleteReactive(cacheKey)
                            .map(v2 -> {
                                logger.info("Invalidated cache for key: {}", cacheKey);
                                logger.info("Successfully permanently deleted banner with id: {}", bannerId);
                                return ApiResponse.<Void>success("Banner deleted permanently!");
                            });
                })
        );
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAllBanner() {
        logger.info("Restoring all trashed banners");

        return tracingMetrics.traceAndMeasure("restoreAllBanner", "restore_all_banners", () -> 
            bannerCommandRepository.restoreAllDeleted()
                .map(success -> {
                    if (!success) {
                        throw new ResourceNotFoundException("No trashed banners found");
                    }
                    logger.info("Successfully restored all trashed banners");
                    return ApiResponse.<Void>success("All banners restored successfully!");
                })
        );
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAllBannerPermanent() {
        logger.info("Permanently deleting all trashed banners");

        return tracingMetrics.traceAndMeasure("deleteAllBannerPermanent", "delete_all_banners_permanent", () -> 
            bannerCommandRepository.deleteAllDeleted()
                .map(success -> {
                    if (!success) {
                        throw new ResourceNotFoundException("No trashed banners found");
                    }
                    logger.info("Successfully permanently deleted all trashed banners");
                    return ApiResponse.<Void>success("All banners permanently deleted!");
                })
        );
    }
}