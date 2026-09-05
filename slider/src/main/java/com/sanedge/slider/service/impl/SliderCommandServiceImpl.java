package com.sanedge.slider.service.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.slider.domain.requests.CreateSliderRequest;
import com.sanedge.slider.domain.requests.UpdateSliderRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.slider.domain.response.SliderResponse;
import com.sanedge.slider.domain.response.SliderResponseDeleteAt;
import com.sanedge.slider.entity.Slider;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.slider.repository.SliderCommandRepository;
import com.sanedge.slider.repository.SliderQueryRepository;
import com.sanedge.slider.service.SliderCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class SliderCommandServiceImpl implements SliderCommandService {
    private static final Logger logger = LoggerFactory.getLogger(SliderCommandServiceImpl.class);

    private final SliderCommandRepository sliderCommandRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    public SliderCommandServiceImpl(SliderCommandRepository sliderCommandRepository,
            SliderQueryRepository sliderQueryRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.sliderCommandRepository = sliderCommandRepository;
        this.validator = validator;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    private <T> void validateRequest(T req) {
        Set<ConstraintViolation<T>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            logger.warn("Validation failed: {}", sb);
            throw new ConstraintViolationException("Validation failed: " + sb, violations);
        }
    }

    private Uni<Void> invalidateSliderCaches() {
        return redisService.deleteReactive("slider:all:*")
                .chain(v -> redisService.deleteReactive("slider:active:*"))
                .chain(v -> redisService.deleteReactive("slider:trashed:*"))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SliderResponse>> createSlider(CreateSliderRequest req) {
        logger.info("Creating slider: {}", req.getNama());

        Attributes attributes = Attributes.builder().put("slider.name", req.getNama()).build();

        return tracingMetrics.traceAndMeasure("createSlider", "create_slider", attributes,
                () -> {
                    validateRequest(req);

                    Slider slider = new Slider();
                    slider.setName(req.getNama());
                    slider.setImage(req.getFilePath());

                    return sliderCommandRepository.persist(slider)
                            .chain(saved -> {
                                SliderResponse response = SliderResponse.from(saved);

                                return invalidateSliderCaches()
                                        .map(v -> {
                                            logger.info("Slider created successfully id={}", saved.id);
                                            return ApiResponse.success("Slider created successfully", response);
                                        });
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SliderResponse>> updateSlider(UpdateSliderRequest req) {
        logger.info("Updating slider ID: {}", req.getId());

        Attributes attributes = Attributes.builder().put("slider.id",
                req.getId() != null ? req.getId().toString() : "null").build();

        return tracingMetrics.traceAndMeasure("updateSlider", "update_slider", attributes,
                () -> {
                    validateRequest(req);

                    return sliderCommandRepository.findById(req.getId().longValue())
                            .chain(slider -> {
                                if (slider == null) {
                                    throw new ResourceNotFoundException("Slider not found");
                                }

                                if (req.getFilePath() != null) {
                                    slider.setImage(req.getFilePath());
                                }

                                slider.setName(req.getNama());
                                return sliderCommandRepository.persist(slider);
                            })
                            .chain(updated -> {
                                SliderResponse response = SliderResponse.from(updated);

                                return invalidateSliderCaches()
                                        .map(v -> {
                                            logger.info("Slider updated successfully id={}", updated.id);
                                            return ApiResponse.success("Slider updated successfully", response);
                                        });
                            });
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SliderResponseDeleteAt>> trashedSlider(Integer sliderId) {
        logger.info("Trashing slider id={}", sliderId);

        Attributes attributes = Attributes.builder().put("slider.id", sliderId.toString()).build();

        return tracingMetrics.traceAndMeasure("trashedSlider", "trash_slider", attributes,
                () -> sliderCommandRepository.trashed(sliderId.longValue())
                        .chain(slider -> {
                            if (slider == null) {
                                throw new ResourceNotFoundException("Slider not found or already trashed");
                            }
                            SliderResponseDeleteAt response = SliderResponseDeleteAt.from(slider);

                            return invalidateSliderCaches()
                                    .map(v -> {
                                        logger.info("Successfully trashed slider with ID: {}", sliderId);
                                        return ApiResponse.success("Slider trashed successfully!", response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SliderResponseDeleteAt>> restoreSlider(Integer sliderId) {
        logger.info("Restoring slider id={}", sliderId);

        Attributes attributes = Attributes.builder().put("slider.id", sliderId.toString()).build();

        return tracingMetrics.traceAndMeasure("restoreSlider", "restore_slider", attributes,
                () -> sliderCommandRepository.restore(sliderId.longValue())
                        .chain(slider -> {
                            if (slider == null) {
                                throw new ResourceNotFoundException("Slider not found or not trashed");
                            }
                            SliderResponseDeleteAt response = SliderResponseDeleteAt.from(slider);

                            return invalidateSliderCaches()
                                    .map(v -> {
                                        logger.info("Successfully restored slider with ID: {}", sliderId);
                                        return ApiResponse.success("Slider restored successfully!", response);
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteSliderPermanent(Integer sliderId) {
        logger.warn("Permanently deleting slider id={}", sliderId);

        Attributes attributes = Attributes.builder().put("slider.id", sliderId.toString()).build();

        return tracingMetrics.traceAndMeasure("deleteSliderPermanent", "delete_slider_permanent", attributes,
                () -> sliderCommandRepository.deletePermanent(sliderId.longValue())
                        .chain(deleted -> {
                            if (deleted == null) {
                                throw new ResourceNotFoundException("Slider not found or not trashed");
                            }
                            return invalidateSliderCaches()
                                    .map(v -> {
                                        logger.info("Successfully permanently deleted slider with ID: {}", sliderId);
                                        return ApiResponse.success("Slider permanently deleted!");
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAllSliders() {
        logger.info("Restoring ALL trashed sliders");

        return tracingMetrics.traceAndMeasure("restoreAllSliders", "restore_all_sliders", Attributes.empty(),
                () -> sliderCommandRepository.restoreAllDeleted()
                        .chain(restored -> {
                            if (!restored) {
                                throw new ResourceNotFoundException("No trashed sliders found");
                            }
                            return invalidateSliderCaches()
                                    .map(v -> {
                                        logger.info("Successfully restored all trashed sliders");
                                        return ApiResponse.success("All sliders restored successfully!");
                                    });
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAllSlidersPermanent() {
        logger.warn("Permanently deleting ALL trashed sliders");

        return tracingMetrics.traceAndMeasure("deleteAllSlidersPermanent", "delete_all_sliders_permanent",
                Attributes.empty(),
                () -> sliderCommandRepository.deleteAllDeleted()
                        .chain(deleted -> {
                            if (!deleted) {
                                throw new ResourceNotFoundException("No trashed sliders found");
                            }
                            return invalidateSliderCaches()
                                    .map(v -> {
                                        logger.info("Successfully permanently deleted all trashed sliders");
                                        return ApiResponse.success("All sliders permanently deleted!");
                                    });
                        }));
    }
}