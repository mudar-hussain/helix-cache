package com.mudar.helixcache.dto;

public record RingNodeResponse (
        long hash,
        String nodeId,
        String address,
        int replicaIndex
) {}
