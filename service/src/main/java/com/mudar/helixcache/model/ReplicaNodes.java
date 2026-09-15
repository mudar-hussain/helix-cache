package com.mudar.helixcache.model;

import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class Replica {
    private String key;
    private Node primaryNode;
    private List<Node> replicaNodes;
}
