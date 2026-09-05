package com.sanedge.product.service.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.repository.MerchantQueryRepository;
import com.sanedge.product.domain.requests.CreateProductRequest;
import com.sanedge.product.domain.requests.UpdateProductRequest;
import com.sanedge.product.domain.response.ProductResponse;
import com.sanedge.product.domain.response.ProductResponseDeleteAt;
import com.sanedge.product.entity.Product;
import com.sanedge.product.repository.ProductCommandRepository;
import com.sanedge.product.repository.ProductQueryRepository;
import com.sanedge.product.service.ProductCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class ProductCommandServiceImpl implements ProductCommandService {
    private static final Logger logger = LoggerFactory.getLogger(ProductCommandServiceImpl.class);

    private final ProductCommandRepository productCommandRepository;
    private final ProductQueryRepository productQueryRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    public ProductCommandServiceImpl(ProductCommandRepository productCommandRepository,
            ProductQueryRepository productQueryRepository,
            MerchantQueryRepository merchantQueryRepository,
            Validator validator,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.productCommandRepository = productCommandRepository;
        this.productQueryRepository = productQueryRepository;
        this.merchantQueryRepository = merchantQueryRepository;
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
            logger.error("Validation failed: {}", sb);
            throw new ConstraintViolationException("Validation failed: " + sb, violations);
        }
    }

    private Uni<Void> invalidateCache(Long productId) {
        if (productId != null) {
            return redisService.deleteReactive("product:id:" + productId).replaceWithVoid();
        }
        return Uni.createFrom().voidItem();
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponse>> createProduct(CreateProductRequest req) {
        logger.info("Creating product: {}", req.getName());

        try {
            validateRequest(req);
        } catch (Exception e) {
            logger.error("Validation failed for create product", e);
            return Uni.createFrom().failure(e);
        }

        Product product = new Product();
        product.setMerchantId(req.getMerchantId());
        product.setCategoryId(req.getCategoryId());
        product.setName(req.getName());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        product.setCountInStock(req.getCountInStock());
        product.setBrand(req.getBrand());
        product.setWeight(req.getWeight());
        product.setRating(req.getRating().floatValue());
        product.setSlugProduct(req.getSlugProduct());
        product.setImageProduct(req.getImageProduct());

        return tracingMetrics.traceAndMeasure("createProduct", "create_product",
                Attributes.builder().put("product.name", req.getName()).build(),
                () -> productCommandRepository.persist(product)
                        .chain(saved -> {
                            ProductResponse response = ProductResponse.from(saved);

                            return invalidateCache(saved.id)
                                    .map(v -> {
                                        logger.info("Successfully created product with ID: {}", saved.id);
                                        return ApiResponse.success("Product created successfully", response);
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to create product: {}", req.getName(), e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponse>> updateProduct(UpdateProductRequest req) {
        logger.info("Updating product ID: {}", req.getProductId());

        try {
            validateRequest(req);
        } catch (Exception e) {
            logger.error("Validation failed for update product", e);
            return Uni.createFrom().failure(e);
        }

        return tracingMetrics.traceAndMeasure("updateProduct", "update_product",
                Attributes.builder().put("product.id", req.getProductId().toString()).build(),
                () -> merchantQueryRepository.findMerchantById(req.getMerchantId().longValue())
                        .chain(merchant -> {
                            if (merchant == null) {
                                throw new ResourceNotFoundException(
                                        "Merchant not found with id " + req.getMerchantId());
                            }
                            return productQueryRepository.findProductById(req.getProductId().longValue());
                        })
                        .chain(optProduct -> {
                            if (optProduct.isEmpty()) {
                                throw new ResourceNotFoundException("Product not found");
                            }
                            Product product = optProduct.get();

                            if (req.getImageProduct() != null) {
                                product.setImageProduct(req.getImageProduct());
                            }

                            product.setMerchantId(req.getMerchantId());
                            product.setCategoryId(req.getCategoryId());
                            product.setName(req.getName());
                            product.setDescription(req.getDescription());
                            product.setPrice(req.getPrice());
                            product.setCountInStock(req.getCountInStock());
                            product.setBrand(req.getBrand());
                            product.setWeight(req.getWeight());
                            product.setRating(req.getRating().floatValue());
                            product.setSlugProduct(req.getSlugProduct());

                            return productCommandRepository.persist(product);
                        })
                        .chain(updated -> {
                            ProductResponse response = ProductResponse.from(updated);

                            return invalidateCache(updated.id)
                                    .map(v -> {
                                        logger.info("Successfully updated product with ID: {}", updated.id);
                                        return ApiResponse.success("Product updated successfully", response);
                                    });
                        })
                        .onFailure()
                        .invoke(e -> logger.error("Failed to update product ID: {}", req.getProductId(), e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponseDeleteAt>> trashedProduct(Integer productId) {
        logger.info("Trashing product ID: {}", productId);

        return tracingMetrics.traceAndMeasure("trashProduct", "trash_product",
                Attributes.builder().put("product.id", productId.toString()).build(),
                () -> productCommandRepository.trashed(productId.longValue())
                        .chain(product -> {
                            if (product == null) {
                                logger.warn("Failed to trash product - not found or already trashed with ID: {}",
                                        productId);
                                throw new ResourceNotFoundException("Product not found or already trashed");
                            }
                            ProductResponseDeleteAt response = ProductResponseDeleteAt.from(product);

                            return invalidateCache(productId.longValue())
                                    .map(v -> {
                                        logger.info("Successfully trashed product with ID: {}", productId);
                                        return ApiResponse.success("Product trashed successfully", response);
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to trash product ID: {}", productId, e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponseDeleteAt>> restoreProduct(Integer productId) {
        logger.info("Restoring product ID: {}", productId);

        return tracingMetrics.traceAndMeasure("restoreProduct", "restore_product",
                Attributes.builder().put("product.id", productId.toString()).build(),
                () -> productCommandRepository.restore(productId.longValue())
                        .chain(product -> {
                            if (product == null) {
                                logger.warn("Failed to restore product - not found or not trashed with ID: {}",
                                        productId);
                                throw new ResourceNotFoundException("Product not found or not trashed");
                            }
                            ProductResponseDeleteAt response = ProductResponseDeleteAt.from(product);

                            return invalidateCache(productId.longValue())
                                    .map(v -> {
                                        logger.info("Successfully restored product with ID: {}", productId);
                                        return ApiResponse.success("Product restored successfully", response);
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to restore product ID: {}", productId, e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponse>> adjustStock(Integer productId, Integer delta) {
        if (productId == null || productId <= 0 || delta == null || delta == 0) {
            return Uni.createFrom().failure(new InvalidRequestException("Product id and non-zero stock delta are required"));
        }

        return tracingMetrics.traceAndMeasure("adjustProductStock", "adjust_product_stock",
                Attributes.builder().put("product.id", productId.toString())
                        .put("product.stock_delta", delta)
                        .build(),
                () -> productCommandRepository.adjustStock(productId.longValue(), delta)
                        .chain(updated -> {
                            if (updated == null) {
                                throw new ResourceNotFoundException(
                                        "Product not found or insufficient stock for product id=" + productId);
                            }
                            return invalidateCache(updated.id)
                                    .map(v -> ApiResponse.success("Product stock adjusted successfully",
                                            ProductResponse.from(updated)));
                        }));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<ProductResponse>> updateProductCountStock(Integer productId, Integer stock) {
        logger.info("Updating product stock ID: {}, stock: {}", productId, stock);

        return tracingMetrics.traceAndMeasure("updateProductCountStock", "update_product_count_stock",
                Attributes.builder().put("product.id", productId.toString()).build(),
                () -> productQueryRepository.findProductById(productId.longValue())
                        .chain(optProduct -> {
                            if (optProduct.isEmpty()) {
                                throw new ResourceNotFoundException("Product not found");
                            }
                            Product product = optProduct.get();
                            product.setCountInStock(stock);

                            return productCommandRepository.persist(product);
                        })
                        .chain(updated -> {
                            ProductResponse response = ProductResponse.from(updated);

                            return invalidateCache(updated.id)
                                    .map(v -> {
                                        logger.info("Successfully updated product stock with ID: {}", updated.id);
                                        return ApiResponse.success("Product stock updated successfully", response);
                                    });
                        })
                        .onFailure().invoke(e -> logger.error("Failed to update product stock ID: {}", productId, e)));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteProductPermanent(Integer productId) {
        Attributes attrs = Attributes.builder().put("product.id", productId.toString()).build();
        logger.warn("Permanently deleting product ID: {}", productId);

        return tracingMetrics.traceAndMeasure("deleteProductPermanent", "delete_product_permanent", attrs, () -> {
            return productCommandRepository.deletePermanent(productId.longValue())
                    .chain(deletedProduct -> {
                        if (deletedProduct == null) {
                            logger.warn("Permanent delete failed - product not found or must be trashed before permanent deletion with id: {}",
                                    productId);
                            throw new InvalidRequestException(
                                    "Product not found or must be trashed before permanent deletion");
                        }

                        return invalidateCache(productId.longValue())
                                .map(v2 -> {
                                    logger.info("Successfully permanently deleted product with ID: {}", productId);
                                    return ApiResponse.success("Product permanently deleted");
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAllProducts() {
        logger.info("Restoring ALL trashed products");

        return tracingMetrics.traceAndMeasure("restoreAllProducts", "restore_all_products", () -> {
            return productCommandRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed products found");
                        }
                        logger.info("Successfully restored all trashed products");
                        return ApiResponse.success("All products restored successfully");
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAllProductsPermanent() {
        logger.warn("Permanently deleting ALL trashed products");

        return tracingMetrics.traceAndMeasure("deleteAllProductsPermanent", "delete_all_products_permanent", () -> {
            return productCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed products found");
                        }
                        logger.info("Successfully permanently deleted all trashed products");
                        return ApiResponse.success("All products permanently deleted");
                    });
        });
    }
}