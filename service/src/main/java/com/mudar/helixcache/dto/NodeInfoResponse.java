package com.mudar.helixcache.dto;

public record NodeInfoResponse(
        String nodeId,
        String host,
        int port,
        int cacheSize
) {
}
