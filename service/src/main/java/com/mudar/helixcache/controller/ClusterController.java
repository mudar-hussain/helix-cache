package com.mudar.helixcache.controller;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.cluster.NodeStateManager;
import com.mudar.helixcache.enums.NodeStatus;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.dto.NodeInfoResponse;
import com.mudar.helixcache.service.CacheService;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cluster")
public class ClusterController {
    private final CacheService cacheService;
    private final ClusterManager clusterManager;
    private final NodeStateManager nodeStateManager;

    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getCacheStats() {
        CacheStats cacheStats = cacheService.getCacheStats();
        return ResponseEntity.ok(cacheStats);
    }

    @GetMapping("/node")
    public ResponseEntity<NodeInfoResponse> getNodeInfo() {
        Node localNode = clusterManager.getLocalNode();
        NodeInfoResponse response = new NodeInfoResponse(
                localNode.id(),
                localNode.host(),
                localNode.port(),
                cacheService.size()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/route/{key}")
    public ResponseEntity<Node> getRouteKey(
            @PathVariable String key
    ) {
        return ResponseEntity.ok(
                clusterManager.getOwner(key)
        );
    }

    @GetMapping("/replicas/{key}")
    public ResponseEntity<List<Node>> getReplicas(@PathVariable String key) {
        return ResponseEntity.ok(clusterManager.getReplicas(key));
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        if(nodeStateManager.isPaused()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "nodeId", clusterManager.getLocalNode().id(),
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
                        "nodeId", clusterManager.getLocalNodeId(),
                        "status", NodeStatus.UP.name(),
                        "timestamp", HelixUtils.getCurrentTimestamp().toString()
                ));
    }
}
