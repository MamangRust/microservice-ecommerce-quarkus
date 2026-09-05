package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.ProductDto;
import com.sanedge.gateway.service.ProductService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductServiceImpl implements ProductService {

    private static final Logger LOG = Logger.getLogger(ProductServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("product")
    pb.product.MutinyProductQueryServiceGrpc.MutinyProductQueryServiceStub productQueryService;

    @GrpcClient("product")
    pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub productCommandService;

    @Override
    public Uni<ProductDto.ApiResponsePaginationProduct> findAll(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("product.findAll", () -> productQueryService.findAll(
                pb.product.ProductQuery.FindAllProductRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ProductDto.ApiResponsePaginationProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find all products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponsePaginationProduct> findByActive(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("product.findByActive", () -> productQueryService.findByActive(
                pb.product.ProductQuery.FindAllProductRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ProductDto.ApiResponsePaginationProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponsePaginationProduct> findByTrashed(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("product.findByTrashed", () -> productQueryService.findByTrashed(
                pb.product.ProductQuery.FindAllProductRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(ProductDto.ApiResponsePaginationProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> findById(int id) {
        return telemetryHelper.traceAndMetric("product.findById", () -> productQueryService.findById(
                pb.product.ProductCommon.FindByIdProductRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find product by id " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> create(pb.product.ProductCommand.CreateProductRequest body) {
        return telemetryHelper.traceAndMetric("product.create", () -> productCommandService.create(body)
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create product: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> update(int id, pb.product.ProductCommand.UpdateProductRequest body) {
        pb.product.ProductCommand.UpdateProductRequest req = pb.product.ProductCommand.UpdateProductRequest.newBuilder(body)
                .setProductId(id)
                .build();
        return telemetryHelper.traceAndMetric("product.update", () -> productCommandService.update(req)
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> delete(int id) {
        return telemetryHelper.traceAndMetric("product.delete", () -> productCommandService.trashedProduct(
                pb.product.ProductCommon.FindByIdProductRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.ApiResponseProduct> restore(int id) {
        return telemetryHelper.traceAndMetric("product.restore", () -> productCommandService.restoreProduct(
                pb.product.ProductCommon.FindByIdProductRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ProductDto.ApiResponseProduct::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.SimpleResponse> deletePermanent(int id) {
        return telemetryHelper.traceAndMetric("product.deletePermanent", () -> productCommandService.deleteProductPermanent(
                pb.product.ProductCommon.FindByIdProductRequest.newBuilder()
                        .setId(id)
                        .build())
                .map(ProductDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete product " + id + ": " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.SimpleResponse> restoreAll() {
        return telemetryHelper.traceAndMetric("product.restoreAll", () -> productCommandService.restoreAllProduct(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ProductDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all products: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ProductDto.SimpleResponse> deleteAllPermanent() {
        return telemetryHelper.traceAndMetric("product.deleteAllPermanent", () -> productCommandService.deleteAllProductPermanent(
                com.google.protobuf.Empty.getDefaultInstance())
                .map(ProductDto.SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all products: " + throwable.getMessage(), throwable)));
    }
}
