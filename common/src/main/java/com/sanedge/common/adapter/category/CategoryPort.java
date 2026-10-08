package com.sanedge.common.adapter.category;

import java.util.List;

import com.sanedge.common.adapter.model.Category;
import com.sanedge.common.adapter.support.Paged;

import io.smallrye.mutiny.Uni;

/**
 * Port baca category. Setara {@code category.QueryRepository} +
 * {@code category.BulkRepository} di Go.
 */
public interface CategoryPort {

    /** @throws com.sanedge.common.exception.ResourceNotFoundException bila tidak ada. */
    Uni<Category> findById(int categoryId);

    Uni<List<Category>> findAllSearch(int page, int pageSize, String search);

    /** Enumerasi berhalaman untuk backfill statistik. */
    Uni<Paged<Category>> findAll(int page, int pageSize);
}
