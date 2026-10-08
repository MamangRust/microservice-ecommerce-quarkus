package com.sanedge.common.adapter.product;

import java.util.List;

import com.sanedge.common.adapter.model.Product;
import com.sanedge.common.adapter.support.Paged;

import io.smallrye.mutiny.Uni;

/**
 * Port product: baca + tulis (stok) + enumerasi. Setara
 * {@code product.QueryRepository}/{@code CommandRepository}/{@code BulkRepository}
 * di Go.
 */
public interface ProductPort {

    /** @throws com.sanedge.common.exception.ResourceNotFoundException bila tidak ada. */
    Uni<Product> findById(int productId);

    Uni<List<Integer>> findIdsByMerchant(int merchantId);

    Uni<Product> updateCountStock(int productId, int stock);

    Uni<Product> adjustStock(int productId, int delta);

    Uni<Paged<Product>> findAll(int page, int pageSize);
}
