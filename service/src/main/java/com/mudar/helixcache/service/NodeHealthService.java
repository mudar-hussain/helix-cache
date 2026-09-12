package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.ClusterEventPublisher;
import com.mudar.helixcache.cluster.NodeHealthTracker;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.enums.ClusterEventType;
import com.mudar.helixcache.enums.NodeStatus;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.model.NodeHealth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class NodeHealthService {

    private final NodeHealthTracker nodeHealthTracker;
    private final RestClient restClient;
    private final ClusterEventPublisher clusterEventPublisher;

    public void addNode(Node node) {
        nodeHealthTracker.register(node);
    }

    public void pingNode(String nodeAddress) {
        restClient
                .get()
                .uri("http://" + nodeAddress + "/cluster/ping")
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, ((request, response) -> {
                    throw new HelixValidationException("Node returned" + response.getStatusCode());
                }))
                .body(String.class);
    }

    public void recordHit(String nodeId) {
        NodeHealth nodeHealth = nodeHealthTracker.getNodeHealth(nodeId);
        if(nodeHealth == null) return;
        NodeStatus previousStatus = nodeHealth.getNodeStatus();
        nodeHealth.recordHit();
        if(previousStatus != NodeStatus.UP) {
            log.info("Node {} is back UP", nodeId);
            clusterEventPublisher.publish(ClusterEventType.NODE_UP, nodeId, "Node recovered", "INFO");
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
            if(currentStatus == NodeStatus.SUSPECT) {
                clusterEventPublisher.publish(ClusterEventType.NODE_SUSPECT, nodeId, "Node suspect: " + nodeHealth.getMissedHeartbeats() + " missed heartbeats", "WARN");
            } else if(currentStatus == NodeStatus.DOWN) {
                clusterEventPublisher.publish(ClusterEventType.NODE_DOWN, nodeId, "Node declared DOWN", "ERROR");
            }
        }

    }

    public boolean isDown(String nodeId) {
        return nodeHealthTracker.isDown(nodeId);
    }

    public NodeStatus getNodeStatus(String nodeId) {
        return nodeHealthTracker.getStatus(nodeId);
    }

    public NodeHealth getNodeHealth(String nodeId) {
        return nodeHealthTracker.getNodeHealth(nodeId);
    }

    public int getRemoteKeyCount(Node node) {
        try {
            CacheStats cacheStats = restClient.get()
                    .uri("http://" + node.address() + "/cluster/stats")
                    .retrieve()
                    .body(CacheStats.class);
            return cacheStats != null ? cacheStats.size() : 0;
        } catch (Exception e) {
            log.warn("Could not fetch key count from node {}: {}", node.id(), e.getMessage());
            return -1; //signals "unreachable"
        }
    }
}
