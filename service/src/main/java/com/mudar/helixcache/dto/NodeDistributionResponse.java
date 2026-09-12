package com.mudar.helixcache.dto;

public record NodeDistributionResponse (
        String nodeId,
        int keyCount,
        double percentage
) { }
