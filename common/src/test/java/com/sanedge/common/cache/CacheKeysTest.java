package com.sanedge.common.cache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CacheKeysTest {

    @Test
    void forList_joinsNamespaceAndParts() {
        assertThat(CacheKeys.forList("order", 1, 10, "active")).isEqualTo("order:1:10:active");
    }

    @Test
    void forList_nullPartBecomesNullString() {
        assertThat(CacheKeys.forList("order", 1, (Object) null)).isEqualTo("order:1:null");
    }

    @Test
    void forStats_joinsParts() {
        assertThat(CacheKeys.forStats("transaction", "stats", 2024, 1)).isEqualTo("transaction:stats:2024:1");
    }

    @Test
    void forEntity_usesIdSuffix() {
        assertThat(CacheKeys.forEntity("order", 42L)).isEqualTo("order:id:42");
    }

    @Test
    void ttlConstantsAreSane() {
        assertThat(CacheKeys.TTL_SHORT_SECONDS).isLessThan(CacheKeys.TTL_DEFAULT_SECONDS);
        assertThat(CacheKeys.TTL_DEFAULT_SECONDS).isLessThan(CacheKeys.TTL_LONG_SECONDS);
    }
}
