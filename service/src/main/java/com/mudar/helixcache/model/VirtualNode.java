package com.mudar.helixcache.model;

public record VirtualNode(
        Node node,
        int replicaIndex,
        long hash
) {
}
