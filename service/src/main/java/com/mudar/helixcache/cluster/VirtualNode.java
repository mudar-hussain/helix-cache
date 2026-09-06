package com.mudar.helixcache.cluster;

public record VirtualNode(
        Node node,
        int replicaIndex,
        long hash
) {
}
