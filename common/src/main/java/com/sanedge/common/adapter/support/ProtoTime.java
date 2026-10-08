package com.sanedge.common.adapter.support;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Konversi timestamp string dari proto ke {@link Instant}.
 *
 * <p>Meniru helper Go {@code parseTime}/{@code parseTimeString}: mengembalikan
 * {@code null} untuk string kosong atau format yang tidak dikenal (tidak pernah
 * melempar error).
 */
public final class ProtoTime {

    private ProtoTime() {
    }

    private static final List<DateTimeFormatter> LENIENT_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public static Instant parse(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (RuntimeException ignored) {
            // coba format berikutnya
        }
        try {
            return Instant.parse(s);
        } catch (RuntimeException ignored) {
            // coba format berikutnya
        }
        for (DateTimeFormatter format : LENIENT_FORMATS) {
            try {
                return LocalDateTime.parse(s, format).toInstant(ZoneOffset.UTC);
            } catch (RuntimeException ignored) {
                // coba format berikutnya
            }
        }
        return null;
    }
}
