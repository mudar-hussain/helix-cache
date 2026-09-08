package com.mudar.helixcache.cluster;

import com.mudar.helixcache.enums.NodeStatus;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.model.NodeHealth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NodeHealthTracker {

    private final Map<String, NodeHealth> healthMap = new ConcurrentHashMap<>();

    public void register(Node node) {
        healthMap.putIfAbsent(node.id(), new NodeHealth(node));
    }

    public NodeHealth getNodeHealth(String nodeId) {
        return healthMap.get(nodeId);
    }

    public boolean isDown(String nodeId) {
        NodeHealth nodeHealth = healthMap.get(nodeId);
        return nodeHealth != null && nodeHealth.isDown();
    }

    public NodeStatus getStatus(String nodeId) {
        NodeHealth nodeHealth = healthMap.get(nodeId);
        return nodeHealth != null ? nodeHealth.getNodeStatus() : NodeStatus.DOWN;
    }

    public Collection<NodeHealth> getAllHealth() {
        return healthMap.values();
    }

}
