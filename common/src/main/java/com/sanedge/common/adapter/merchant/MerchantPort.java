package com.sanedge.common.adapter.merchant;

import com.sanedge.common.adapter.model.Merchant;

import io.smallrye.mutiny.Uni;

/**
 * Port baca merchant. Setara {@code merchant.QueryRepository} di Go: konsumen
 * bergantung pada kontrak ini, bukan pada stub gRPC.
 */
public interface MerchantPort {

    /** @throws com.sanedge.common.exception.ResourceNotFoundException bila merchant tidak ada. */
    Uni<Merchant> findById(int merchantId);
}
