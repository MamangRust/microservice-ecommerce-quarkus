package com.sanedge.product.handler;

import com.sanedge.product.service.ProductCommandService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.product.MutinyProductCommandServiceGrpc;
import pb.product.ProductCommon.ApiResponseProduct;
import pb.product.ProductCommon.ApiResponseProductAll;
import pb.product.ProductCommon.ApiResponseProductDelete;
import pb.product.ProductCommon.ApiResponseProductDeleteAt;
import pb.product.ProductCommon.FindByIdProductRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class ProductCommandGrpcHandler extends MutinyProductCommandServiceGrpc.ProductCommandServiceImplBase {

    @Inject
    ProductCommandService productCommandService;

    @Override
    public Uni<ApiResponseProduct> create(pb.product.ProductCommand.CreateProductRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        if (request.getCategoryId() <= 0) {
            return IdValidator.invalid("Category id");
        }
        com.sanedge.product.domain.requests.CreateProductRequest domainReq = new com.sanedge.product.domain.requests.CreateProductRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());
        domainReq.setPrice(request.getPrice());
        domainReq.setCountInStock(request.getCountInStock());
        domainReq.setBrand(request.getBrand());
        domainReq.setWeight(request.getWeight());
        domainReq.setRating(request.getRating());
        domainReq.setSlugProduct(request.getSlugProduct());
        domainReq.setImageProduct(request.getImageProduct());

        return productCommandService.createProduct(domainReq)
                .map(apiResp -> {
                    ApiResponseProduct.Builder builder = ApiResponseProduct.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceAlreadyExistsException) {
                        return Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseProduct> update(pb.product.ProductCommand.UpdateProductRequest request) {
        if (request.getProductId() <= 0) {
            return IdValidator.invalid("Product id");
        }
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        if (request.getCategoryId() <= 0) {
            return IdValidator.invalid("Category id");
        }
        com.sanedge.product.domain.requests.UpdateProductRequest domainReq = new com.sanedge.product.domain.requests.UpdateProductRequest();
        domainReq.setProductId(request.getProductId());
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setName(request.getName());
        domainReq.setDescription(request.getDescription());
        domainReq.setPrice(request.getPrice());
        domainReq.setCountInStock(request.getCountInStock());
        domainReq.setBrand(request.getBrand());
        domainReq.setWeight(request.getWeight());
        domainReq.setRating(request.getRating());
        domainReq.setSlugProduct(request.getSlugProduct());
        domainReq.setImageProduct(request.getImageProduct());

        return productCommandService.updateProduct(domainReq)
                .map(apiResp -> {
                    ApiResponseProduct.Builder builder = ApiResponseProduct.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseProduct> updateProductCountStock(
            pb.product.ProductCommand.UpdateProductCountStockRequest request) {
        if (request.getProductId() <= 0) {
            return IdValidator.invalid("Product id");
        }
        return productCommandService.updateProductCountStock(request.getProductId(), request.getStock())
                .map(apiResp -> {
                    ApiResponseProduct.Builder builder = ApiResponseProduct.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseProduct> adjustStock(pb.product.ProductCommand.AdjustProductStockRequest request) {
        if (request.getProductId() <= 0) {
            return IdValidator.invalid("Product id");
        }
        return productCommandService.adjustStock(request.getProductId(), request.getDelta())
                .map(apiResp -> {
                    ApiResponseProduct.Builder builder = ApiResponseProduct.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseProductDeleteAt> trashedProduct(FindByIdProductRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return productCommandService.trashedProduct(request.getId())
                .map(apiResp -> {
                    ApiResponseProductDeleteAt.Builder builder = ApiResponseProductDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseProductDeleteAt> restoreProduct(FindByIdProductRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return productCommandService.restoreProduct(request.getId())
                .map(apiResp -> {
                    ApiResponseProductDeleteAt.Builder builder = ApiResponseProductDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseProductDelete> deleteProductPermanent(FindByIdProductRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return productCommandService.deleteProductPermanent(request.getId())
                .map(apiResp -> ApiResponseProductDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return GrpcErrorMapper.toStatusRuntimeException(e);
                });
    }

    @Override
    public Uni<ApiResponseProductAll> restoreAllProduct(com.google.protobuf.Empty request) {
        return productCommandService.restoreAllProducts()
                .map(apiResp -> ApiResponseProductAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseProductAll> deleteAllProductPermanent(com.google.protobuf.Empty request) {
        return productCommandService.deleteAllProductsPermanent()
                .map(apiResp -> ApiResponseProductAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    private pb.product.ProductCommon.ProductResponse toProto(com.sanedge.product.domain.response.ProductResponse r) {
        if (r == null) {
            return pb.product.ProductCommon.ProductResponse.getDefaultInstance();
        }
        pb.product.ProductCommon.ProductResponse.Builder builder = pb.product.ProductCommon.ProductResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getCategoryId() != null) {
            builder.setCategoryId(r.getCategoryId());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getPrice() != null) {
            builder.setPrice(r.getPrice());
        }
        if (r.getCountInStock() != null) {
            builder.setCountInStock(r.getCountInStock());
        }
        if (r.getBrand() != null) {
            builder.setBrand(r.getBrand());
        }
        if (r.getWeight() != null) {
            builder.setWeight(r.getWeight());
        }
        if (r.getRating() != null) {
            builder.setRating(r.getRating());
        }
        if (r.getSlugProduct() != null) {
            builder.setSlugProduct(r.getSlugProduct());
        }
        if (r.getImageProduct() != null) {
            builder.setImageProduct(r.getImageProduct());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.product.ProductCommon.ProductResponseDeleteAt toProto(
            com.sanedge.product.domain.response.ProductResponseDeleteAt r) {
        if (r == null) {
            return pb.product.ProductCommon.ProductResponseDeleteAt.getDefaultInstance();
        }
        pb.product.ProductCommon.ProductResponseDeleteAt.Builder builder = pb.product.ProductCommon.ProductResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getCategoryId() != null) {
            builder.setCategoryId(r.getCategoryId());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getDescription() != null) {
            builder.setDescription(r.getDescription());
        }
        if (r.getPrice() != null) {
            builder.setPrice(safeParseInt(r.getPrice()));
        }
        if (r.getCountInStock() != null) {
            builder.setCountInStock(safeParseInt(r.getCountInStock()));
        }
        if (r.getBrand() != null) {
            builder.setBrand(r.getBrand());
        }
        if (r.getWeight() != null) {
            builder.setWeight(safeParseInt(r.getWeight()));
        }
        if (r.getRating() != null) {
            builder.setRating(safeParseFloat(r.getRating()));
        }
        if (r.getSlugProduct() != null) {
            builder.setSlugProduct(r.getSlugProduct());
        }
        if (r.getImageProduct() != null) {
            builder.setImageProduct(r.getImageProduct());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }

    private int safeParseInt(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private float safeParseFloat(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("null")) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }
}
