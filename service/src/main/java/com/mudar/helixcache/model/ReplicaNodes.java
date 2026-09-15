package com.mudar.helixcache.model;

import lombok.AllArgsConstructor;

import java.util.List;

public record ReplicaNodes (
        String key,
        Node primaryNode,
        List<Node> replicaNodes
) {
}
