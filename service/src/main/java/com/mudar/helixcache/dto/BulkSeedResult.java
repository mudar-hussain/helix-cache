package com.mudar.helixcache.dto;

public record BulkSeedResult(
        int total,
        int succeeded,
        int failed
) {
}
