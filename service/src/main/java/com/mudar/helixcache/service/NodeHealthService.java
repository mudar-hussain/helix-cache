package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.NodeHealthTracker;
import com.mudar.helixcache.enums.NodeStatus;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.model.NodeHealth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodeHealthService {

    private NodeHealthTracker nodeHealthTracker;
    private final RestClient restClient;

    public void addNode(Node node) {
        nodeHealthTracker.register(node);
    }

    public void pingNode(String nodeAddress) {
        restClient
                .get()
                .uri("http://" + nodeAddress + "/cluster/ping")
                .retrieve()
                .body(String.class);
    }

    public void recordHit(String nodeId) {
        NodeHealth nodeHealth = nodeHealthTracker.getNodeHealth(nodeId);
        if(nodeHealth == null) return;
        NodeStatus previousStatus = nodeHealth.getNodeStatus();
        nodeHealth.recordHit();
        if(previousStatus != NodeStatus.UP) {
            log.info("Node {} is back UP", nodeId);
        }
    }

    public void recordMiss(String nodeId) {
        NodeHealth nodeHealth = nodeHealthTracker.getNodeHealth(nodeId);
        if(nodeHealth == null) return;
        NodeStatus previousStatus = nodeHealth.getNodeStatus();
        nodeHealth.recordMiss();
        NodeStatus currentStatus = nodeHealth.getNodeStatus();

        if(previousStatus != currentStatus) {
            log.warn("Node {} status changed: {} -> {} (missed heartbeats: {})",
                    nodeId, previousStatus, currentStatus, nodeHealth.getMissedHeartbeats());
        }
    }

    public boolean isDown(String nodeId) {
        return nodeHealthTracker.isDown(nodeId);


    }
}
