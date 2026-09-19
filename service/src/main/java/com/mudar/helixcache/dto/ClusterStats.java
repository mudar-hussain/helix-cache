package com.mudar.helixcache.dto;

public record ClusterStats (
        int totalKeys,
        int replicationFactor,
        int writeQuorum,
        int readQuorum,
        int virtualNodesPerNode
) {
}
