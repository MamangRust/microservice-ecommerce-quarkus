package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.ProductDto;
import io.smallrye.mutiny.Uni;

public interface ProductService {
    Uni<ProductDto.ApiResponsePaginationProduct> findAll(int page, int size, String search);
    Uni<ProductDto.ApiResponsePaginationProduct> findByActive(int page, int size, String search);
    Uni<ProductDto.ApiResponsePaginationProduct> findByTrashed(int page, int size, String search);
    Uni<ProductDto.ApiResponseProduct> findById(int id);
    Uni<ProductDto.ApiResponseProduct> create(pb.product.ProductCommand.CreateProductRequest body);
    Uni<ProductDto.ApiResponseProduct> update(int id, pb.product.ProductCommand.UpdateProductRequest body);
    Uni<ProductDto.ApiResponseProduct> delete(int id);
    Uni<ProductDto.ApiResponseProduct> restore(int id);
    Uni<ProductDto.SimpleResponse> deletePermanent(int id);
    Uni<ProductDto.SimpleResponse> restoreAll();
    Uni<ProductDto.SimpleResponse> deleteAllPermanent();
}
