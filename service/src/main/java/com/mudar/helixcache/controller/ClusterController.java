package com.mudar.helixcache.controller;

import com.mudar.helixcache.cluster.NodeStateManager;
import com.mudar.helixcache.dto.*;
import com.mudar.helixcache.enums.NodeStatus;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.service.CacheService;
import com.mudar.helixcache.service.ClusterService;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/cluster")
public class ClusterController {
    private final CacheService cacheService;
    private final NodeStateManager nodeStateManager;
    private final ClusterService clusterService;

    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getCacheStats() {
        return ResponseEntity.ok(cacheService.getCacheStats());
    }

    @GetMapping("/node")
    public ResponseEntity<NodeInfoResponse> getNodeInfo() {
        return ResponseEntity.ok(clusterService.getNodeInfo());
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<NodeStatusResponse>> getNodeStatus() {
        return ResponseEntity.ok(clusterService.getNodeStatusResponseList());
    }

    @GetMapping("/route/{key}")
    public ResponseEntity<Node> getRouteKey(@PathVariable String key) {
        return ResponseEntity.ok(clusterService.getOwner(key));
    }

    @GetMapping("/replicas/{key}")
    public ResponseEntity<List<Node>> getReplicas(@PathVariable String key) {
        return ResponseEntity.ok(clusterService.getReplicas(key));
    }

    @GetMapping("/ring")
    public ResponseEntity<List<RingNodeResponse>> getRingNodeResponse() {
        return ResponseEntity.ok(clusterService.getRingNode());
    }

    @GetMapping("/ring/nodes")
    public ResponseEntity<List<Node>> getActiveNodes() {
        return ResponseEntity.ok(new ArrayList<>(clusterService.getActiveNodes()));
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        if(nodeStateManager.isPaused()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "nodeId", clusterService.getLocalNodeId(),
                            "status", NodeStatus.DOWN.name(),
                            "reason", "Node is paused (simulated failure)"
                    ));
        }
        long delayMs = nodeStateManager.getSlowDelayMs();
        if(delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return ResponseEntity.ok(
                Map.of(
                        "nodeId", clusterService.getLocalNodeId(),
                        "status", NodeStatus.UP.name(),
                        "timestamp", HelixUtils.getCurrentTimestamp().toString()
                ));
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<NodeDistributionResponse>> getClusterDistribution() {
        return ResponseEntity.ok(clusterService.getNodeDistributionResponseList());
    }
}
