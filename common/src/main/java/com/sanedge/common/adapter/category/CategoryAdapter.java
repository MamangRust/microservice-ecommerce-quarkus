package com.sanedge.common.adapter.category;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.common.adapter.model.Category;
import com.sanedge.common.adapter.support.Paged;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.category.CategoryCommon.CategoryResponse;
import pb.category.CategoryQuery.FindAllCategoryRequest;
import pb.category.CategoryCommon.FindByIdCategoryRequest;
import pb.category.MutinyCategoryQueryServiceGrpc.MutinyCategoryQueryServiceStub;

@ApplicationScoped
public class CategoryAdapter implements CategoryPort {

    @GrpcClient("category")
    MutinyCategoryQueryServiceStub client;

    @Override
    public Uni<Category> findById(int categoryId) {
        return client.findById(FindByIdCategoryRequest.newBuilder().setId(categoryId).build())
                .map(resp -> {
                    if (!resp.hasData()) {
                        throw new ResourceNotFoundException("Category not found: " + categoryId);
                    }
                    return toModel(resp.getData());
                });
    }

    @Override
    public Uni<List<Category>> findAllSearch(int page, int pageSize, String search) {
        return client.findAll(FindAllCategoryRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .setSearch(search == null ? "" : search)
                .build())
                .map(resp -> toModels(resp.getDataList()));
    }

    @Override
    public Uni<Paged<Category>> findAll(int page, int pageSize) {
        return client.findAll(FindAllCategoryRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build())
                .map(resp -> {
                    List<Category> items = toModels(resp.getDataList());
                    int total = resp.hasPagination() ? resp.getPagination().getTotalRecords() : items.size();
                    return Paged.of(items, total);
                });
    }

    static Category toModel(CategoryResponse c) {
        if (c == null) {
            return null;
        }
        return new Category(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getSlugCategory(),
                c.getImageCategory(),
                ProtoTime.parse(c.getCreatedAt()),
                ProtoTime.parse(c.getUpdatedAt()));
    }

    static List<Category> toModels(List<CategoryResponse> list) {
        List<Category> out = new ArrayList<>(list.size());
        for (CategoryResponse c : list) {
            out.add(toModel(c));
        }
        return out;
    }
}
