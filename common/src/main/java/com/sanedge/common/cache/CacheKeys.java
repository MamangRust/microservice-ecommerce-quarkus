package com.sanedge.common.cache;

/**
 * Standardized cache key namespace and TTL conventions.
 *
 * <p>All cache keys must be built through this helper so the format stays
 * consistent across services ({@code namespace:p1:p2:...} for lists/stats,
 * {@code namespace:id:<id>} for single entities). This keeps eviction and
 * Redis cluster key naming predictable.</p>
 */
public final class CacheKeys {

    private CacheKeys() {
    }

    /** Short-lived data (fast-moving state). */
    public static final long TTL_SHORT_SECONDS = 60;
    /** Default list/stats TTL (5 minutes). */
    public static final long TTL_DEFAULT_SECONDS = 300;
    /** Long-lived reference data (10 minutes). */
    public static final long TTL_LONG_SECONDS = 600;

    /** Builds {@code namespace:p1:p2:...} for paginated lists and stats. */
    public static String forList(String namespace, Object... parts) {
        return join(namespace, parts);
    }

    /** Builds {@code namespace:p1:p2:...} for stats/aggregation queries. */
    public static String forStats(String namespace, Object... parts) {
        return join(namespace, parts);
    }

    /** Builds {@code namespace:id:<id>} for single-entity caches. */
    public static String forEntity(String namespace, Object id) {
        return namespace + ":id:" + id;
    }

    private static String join(String namespace, Object... parts) {
        StringBuilder sb = new StringBuilder(namespace);
        for (Object part : parts) {
            sb.append(':').append(part);
        }
        return sb.toString();
    }
}
