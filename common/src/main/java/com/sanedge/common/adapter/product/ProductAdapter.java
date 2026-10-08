package com.sanedge.common.adapter.product;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.common.adapter.model.Product;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.adapter.support.Paged;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.product.MutinyProductCommandServiceGrpc.MutinyProductCommandServiceStub;
import pb.product.MutinyProductQueryServiceGrpc.MutinyProductQueryServiceStub;
import pb.product.ProductCommand.AdjustProductStockRequest;
import pb.product.ProductCommand.UpdateProductCountStockRequest;
import pb.product.ProductCommon.ApiResponseProduct;
import pb.product.ProductCommon.FindByIdProductRequest;
import pb.product.ProductCommon.ProductResponse;
import pb.product.ProductQuery.FindAllProductMerchantRequest;
import pb.product.ProductQuery.FindAllProductRequest;

@ApplicationScoped
public class ProductAdapter implements ProductPort {

    @GrpcClient("product")
    MutinyProductQueryServiceStub query;

    @GrpcClient("product")
    MutinyProductCommandServiceStub command;

    @Override
    public Uni<Product> findById(int productId) {
        return query.findById(FindByIdProductRequest.newBuilder().setId(productId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Product not found: " + productId);
                    }
                    return toModel(resp.getData());
                });
    }

    @Override
    public Uni<List<Integer>> findIdsByMerchant(int merchantId) {
        return query.findByMerchant(FindAllProductMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .setPage(1)
                .setPageSize(100_000)
                .build())
                .map(resp -> {
                    List<Integer> ids = new ArrayList<>(resp.getDataList().size());
                    for (ProductResponse p : resp.getDataList()) {
                        ids.add(p.getId());
                    }
                    return ids;
                });
    }

    @Override
    public Uni<Product> updateCountStock(int productId, int stock) {
        return command.updateProductCountStock(UpdateProductCountStockRequest.newBuilder()
                .setProductId(productId)
                .setStock(stock)
                .build())
                .map(resp -> requireData(resp, "Failed to update product stock: " + productId));
    }

    @Override
    public Uni<Product> adjustStock(int productId, int delta) {
        return command.adjustStock(AdjustProductStockRequest.newBuilder()
                .setProductId(productId)
                .setDelta(delta)
                .build())
                .map(resp -> requireData(resp, "Failed to adjust product stock: " + productId));
    }

    @Override
    public Uni<Paged<Product>> findAll(int page, int pageSize) {
        return query.findAll(FindAllProductRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build())
                .map(resp -> {
                    List<Product> items = toModels(resp.getDataList());
                    int total = resp.hasPagination() ? resp.getPagination().getTotalRecords() : items.size();
                    return Paged.of(items, total);
                });
    }

    private static Product requireData(ApiResponseProduct resp, String message) {
        if (resp == null || !resp.hasData()) {
            throw new AdapterException(message);
        }
        return toModel(resp.getData());
    }

    static Product toModel(ProductResponse p) {
        if (p == null) {
            return null;
        }
        return new Product(
                p.getId(),
                p.getMerchantId(),
                p.getCategoryId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCountInStock(),
                p.getBrand(),
                p.getWeight(),
                p.getRating(),
                p.getSlugProduct(),
                p.getImageProduct(),
                ProtoTime.parse(p.getCreatedAt()),
                ProtoTime.parse(p.getUpdatedAt()));
    }

    static List<Product> toModels(List<ProductResponse> list) {
        List<Product> out = new ArrayList<>(list.size());
        for (ProductResponse p : list) {
            out.add(toModel(p));
        }
        return out;
    }
}
