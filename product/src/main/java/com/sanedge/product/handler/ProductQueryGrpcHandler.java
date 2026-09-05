package com.sanedge.product.handler;

import com.sanedge.product.service.ProductQueryService;

import io.grpc.Status;
import com.sanedge.common.grpc.GrpcErrorMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.product.MutinyProductQueryServiceGrpc;
import pb.product.ProductCommon.ApiResponsePaginationProduct;
import pb.product.ProductCommon.ApiResponsePaginationProductDeleteAt;
import pb.product.ProductCommon.ApiResponseProduct;
import pb.product.ProductCommon.FindByIdProductRequest;
import com.sanedge.common.utils.IdValidator;

@GrpcService
@Singleton
public class ProductQueryGrpcHandler extends MutinyProductQueryServiceGrpc.ProductQueryServiceImplBase {

    @Inject
    ProductQueryService productQueryService;

    @Override
    @WithSession
    public Uni<ApiResponsePaginationProduct> findAll(pb.product.ProductQuery.FindAllProductRequest request) {
        com.sanedge.product.domain.requests.FindAllProductRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return productQueryService.findAll(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProduct.Builder builder = ApiResponsePaginationProduct.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (com.sanedge.product.domain.response.ProductResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationProduct> findByMerchant(
            pb.product.ProductQuery.FindAllProductMerchantRequest request) {
        if (request.getMerchantId() <= 0) {
            return IdValidator.invalid("Merchant id");
        }
        if (request.getCategoryId() <= 0) {
            return IdValidator.invalid("Category id");
        }
        com.sanedge.product.domain.requests.FindAllProductByMerchantRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductByMerchantRequest();
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setCategoryId(request.getCategoryId());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());
        domainReq.setMinPrice(request.getMinPrice());
        domainReq.setMaxPrice(request.getMaxPrice());

        return productQueryService.findByMerchant(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProduct.Builder builder = ApiResponsePaginationProduct.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (com.sanedge.product.domain.response.ProductResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationProduct> findByCategory(
            pb.product.ProductQuery.FindAllProductCategoryRequest request) {
        com.sanedge.product.domain.requests.FindAllProductByCategoryRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductByCategoryRequest();
        domainReq.setCategoryName(request.getCategoryName());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());
        domainReq.setMinPrice(request.getMinPrice());
        domainReq.setMaxPrice(request.getMaxPrice());

        return productQueryService.findByCategoryName(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProduct.Builder builder = ApiResponsePaginationProduct.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (com.sanedge.product.domain.response.ProductResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponseProduct> findById(FindByIdProductRequest request) {
        if (request.getId() <= 0) {
            return IdValidator.invalid("id");
        }
        return productQueryService.findById((long) request.getId())
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
    @WithSession
    public Uni<ApiResponsePaginationProductDeleteAt> findByActive(
            pb.product.ProductQuery.FindAllProductRequest request) {
        com.sanedge.product.domain.requests.FindAllProductRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return productQueryService.findActiveProducts(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProductDeleteAt.Builder builder = ApiResponsePaginationProductDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (com.sanedge.product.domain.response.ProductResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcErrorMapper.toStatusRuntimeException(e));
    }

    @Override
    @WithSession
    public Uni<ApiResponsePaginationProductDeleteAt> findByTrashed(
            pb.product.ProductQuery.FindAllProductRequest request) {
        com.sanedge.product.domain.requests.FindAllProductRequest domainReq = new com.sanedge.product.domain.requests.FindAllProductRequest();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return productQueryService.findTrashedProducts(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationProductDeleteAt.Builder builder = ApiResponsePaginationProductDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (com.sanedge.product.domain.response.ProductResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPagination(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
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

    private pb.Api.PaginationMeta toProto(com.sanedge.common.domain.response.PaginationMeta m) {
        if (m == null) {
            return pb.Api.PaginationMeta.getDefaultInstance();
        }
        return pb.Api.PaginationMeta.newBuilder()
                .setCurrentPage(m.currentPage())
                .setPageSize(m.pageSize())
                .setTotalPages(m.totalPages())
                .setTotalRecords(m.totalRecords())
                .build();
    }
}
