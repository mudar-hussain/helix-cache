package com.mudar.helixcache.cluster;

import com.mudar.helixcache.config.ClusterProperties;
import com.mudar.helixcache.config.NodeProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class ClusterManager {

    private final NodeProperties nodeProperties;
    private final ClusterProperties clusterProperties;
    private final ConsistentHashRing hashRing = new ConsistentHashRing();

    @PostConstruct
    private void initialize() {
        clusterProperties.getNodes().forEach(hashRing::addNode);
        hashRing.getVirtualNodes().stream()
                .limit(10)
                .forEach(v ->
                        System.out.println(
                                v.hash() + " -> " + v.node().id()
                        )
                );
    }

    public Node getOwner(String key) {
        return hashRing.getNode(key);
    }

    public Node getLocalNode() {
        return new Node(nodeProperties.getId(), nodeProperties.getHost(), nodeProperties.getPort());
    }
}
