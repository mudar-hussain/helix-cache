package com.mudar.helixcache.cluster;

import com.mudar.helixcache.config.ClusterProperties;
import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.model.Node;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Getter
@RequiredArgsConstructor
public class ClusterManager {

    private final NodeProperties nodeProperties;
    private final ClusterProperties clusterProperties;
    private final ConsistentHashRing hashRing = new ConsistentHashRing();
    private Node localNode;

    @PostConstruct
    private void initialize() {
        this.localNode = new Node(nodeProperties.getId(), nodeProperties.getHost(), nodeProperties.getPort());
        clusterProperties.getNodes().forEach(hashRing::addNode);
        hashRing.getVirtualNodes().stream()
                .limit(10)
                .forEach(v ->
                        log.info("Virtual node: hash={} -> nodeId={} address={}", v.hash(), v.node().id(), v.node().address())
                );

    }

    public Node getOwner(String key) {
        return hashRing.getNode(key);
    }

    public List<Node> getReplicas(String key) {
        return hashRing.getReplicaNodes(key, clusterProperties.getReplicationFactor());
    }

}
