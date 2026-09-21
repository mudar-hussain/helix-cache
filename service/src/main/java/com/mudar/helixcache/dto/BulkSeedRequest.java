package com.mudar.helixcache.dto;

public record BulkSeedRequest(
        int count,
        String prefix
) {
}
