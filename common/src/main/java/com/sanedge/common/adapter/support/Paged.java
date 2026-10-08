package com.sanedge.common.adapter.support;

import java.util.List;

/**
 * Hasil enumerasi berhalaman: daftar item + total record yang dilaporkan
 * service. Setara {@code BulkRepository.FindAll(...) ([]*models.X, int, error)}
 * di Go.
 */
public record Paged<T>(List<T> items, int total) {

    public static <T> Paged<T> of(List<T> items, int total) {
        return new Paged<>(items == null ? List.of() : items, total);
    }
}
