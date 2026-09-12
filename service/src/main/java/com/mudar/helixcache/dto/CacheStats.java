package com.mudar.helixcache.dto;

public record CacheStats(
        int size,
        long hitCount,
        long missCount,
        double hitRatio
) {
}
