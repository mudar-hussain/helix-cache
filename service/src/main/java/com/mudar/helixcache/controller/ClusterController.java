package com.mudar.helixcache.controller;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.dto.NodeInfoResponse;
import com.mudar.helixcache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cluster")
public class ClusterController {
    private final CacheService cacheService;
    private final NodeProperties nodeProperties;
    private final ClusterManager clusterManager;

    @GetMapping("/stats")
    public ResponseEntity<CacheStats> getCacheStats() {
        CacheStats cacheStats = cacheService.getCacheStats();
        return ResponseEntity.ok(cacheStats);
    }

    @GetMapping("/node")
    public ResponseEntity<NodeInfoResponse> getNodeInfo() {

        NodeInfoResponse response = new NodeInfoResponse(
                nodeProperties.getId(),
                nodeProperties.getHost(),
                nodeProperties.getPort(),
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
}
