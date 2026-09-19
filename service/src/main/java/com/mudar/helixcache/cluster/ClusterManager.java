package com.mudar.helixcache.cluster;

import com.mudar.helixcache.cluster.hashfunction.Sha256HashFunction;
import com.mudar.helixcache.config.ClusterProperties;
import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.dto.NodeDistributionResponse;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.model.VirtualNode;
import com.mudar.helixcache.service.NodeHealthService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterManager {

    private final NodeProperties nodeProperties;
    private final ClusterProperties clusterProperties;
    private final NodeHealthService nodeHealthService;
    private ConsistentHashRing hashRing;
    private Node localNode = null;

    @PostConstruct
    private void initialize() {
        this.hashRing = new ConsistentHashRing(
                clusterProperties.getVirtualNodesPerNode(),
                new Sha256HashFunction()
        );
        this.localNode = new Node(nodeProperties.getId(), nodeProperties.getHost(), nodeProperties.getPort());
        clusterProperties.getNodes().forEach(node -> {
            hashRing.addNode(node);
            if (!node.id().equals(localNode.id())) {
                nodeHealthService.addNode(node);
            }
        });

        hashRing.getVirtualNodes().stream()
                .limit(10)
                .forEach(v ->
                        log.info("Virtual node: hash={} -> nodeId={} address={}",
                                v.hash(), v.node().id(), v.node().address())
                );
    }

    public Node getOwner(String key) {
        return hashRing.getNode(key);
    }

    public List<Node> getReplicas(String key) {
        return hashRing.getReplicaNodes(key, clusterProperties.getReplicationFactor());
    }

    public List<Node> getNodes() {
        return clusterProperties.getNodes();
    }

    public boolean containsNode(String nodeId) {
        return hashRing.containsNode(nodeId);
    }

    public void addNode(Node node) {
        hashRing.addNode(node);
    }

    public void removeNode(String nodeId) {
        hashRing.removeNode(nodeId);
    }

    public int getWriteQuorum() {
        return clusterProperties.getWriteQuorum();
    }

    public int getReadQuorum() {
        return clusterProperties.getReadQuorum();
    }

    public int getReplicationFactor() {
        return clusterProperties.getReplicationFactor();
    }

    public int getVirtualNodesPerNode() {
        return clusterProperties.getVirtualNodesPerNode();
    }

    public String getLocalNodeId() {
        return localNode.id();
    }

    public Node getLocalNode() {
        return localNode;
    }

    public List<VirtualNode> getRingNodes() {
        return hashRing.getVirtualNodes();
    }

    public Collection<Node> getActiveNodes() {
        return hashRing.getNodes();
    }

}
