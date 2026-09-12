package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.dto.NodeDistributionResponse;
import com.mudar.helixcache.dto.NodeInfoResponse;
import com.mudar.helixcache.dto.NodeStatusResponse;
import com.mudar.helixcache.dto.RingNodeResponse;
import com.mudar.helixcache.enums.NodeStatus;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.model.NodeHealth;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterManager clusterManager;
    private final CacheService cacheService;
    private final NodeHealthService nodeHealthService;


    public List<NodeDistributionResponse> getNodeDistributionResponseList() {
        List<Node> allNodes = clusterManager.getNodes();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for(Node node: allNodes) {
            if (node.id().equals(clusterManager.getLocalNodeId())) {
                counts.put(node.id(), cacheService.size());
            } else {
                counts.put(node.id(), nodeHealthService.getRemoteKeyCount(node));
            }
        }
        int total = counts.values().stream().mapToInt(Integer::intValue).sum();

        List<NodeDistributionResponse> result = counts.entrySet().stream()
                .map(entry -> new NodeDistributionResponse(
                        entry.getKey(),
                        entry.getValue(),
                        total == 0 ? 0.0 : (double) entry.getValue() / total * 100
                ))
                .toList();

        return result;
    }

    public List<NodeStatusResponse> getNodeStatusResponseList() {
        String localNodeId = this.getLocalNodeId();
        List<NodeStatusResponse> nodeStatusResponseList = this.getNodes().stream()
                .map(node -> {
                    boolean isLocal = node.id().equals(localNodeId);
                    NodeStatus nodeStatus = isLocal ? NodeStatus.UP : nodeHealthService.getNodeStatus(node.id());
                    int keyCount = isLocal ? cacheService.size() : nodeStatus != NodeStatus.DOWN ? nodeHealthService.getRemoteKeyCount(node) : 0;
                    NodeHealth nodeHealth = nodeHealthService.getNodeHealth(node.id());
                    int missedHeartbeats = nodeHealth != null ? nodeHealth.getMissedHeartbeats() : 0;
                    Timestamp lastSeenAt = nodeHealth != null ? nodeHealth.getLastSeenAt() : HelixUtils.getCurrentTimestamp();
                    return new NodeStatusResponse(node.id(), node.address(), nodeStatus, isLocal, keyCount, missedHeartbeats, lastSeenAt);
                })
                .toList();

        return nodeStatusResponseList;
    }

    public NodeInfoResponse getNodeInfo() {
        Node localNode = this.getLocalNode();
        return new NodeInfoResponse(
                localNode.id(),
                localNode.host(),
                localNode.port(),
                cacheService.size()
        );
    }

    public List<RingNodeResponse> getRingNode() {
        List<RingNodeResponse> ringNodeResponseList = clusterManager.getRingNodes().stream()
                .map(v -> {
                    return new RingNodeResponse(
                            v.hash(),
                            v.node().id(),
                            v.node().address(),
                            v.replicaIndex()
                    );
                })
                .toList();
        return ringNodeResponseList;
    }

    public List<Node> getReplicas(String key) {
        return clusterManager.getReplicas(key);
    }

    public Node getLocalNode() {
        return clusterManager.getLocalNode();
    }

    public String getLocalNodeId() {
        return clusterManager.getLocalNode().id();
    }

    public List<Node> getNodes() {
        return clusterManager.getNodes();
    }

    public Node getOwner(String key) {
        return clusterManager.getOwner(key);
    }

    public Collection<Node> getActiveNodes() {
        return clusterManager.getActiveNodes();
    }
}
