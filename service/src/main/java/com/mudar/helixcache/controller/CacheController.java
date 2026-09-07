package com.mudar.helixcache.controller;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.cluster.Node;
import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.dto.NodeInfoResponse;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;
    private final NodeProperties nodeProperties;
    private final ClusterManager clusterManager;

    @PutMapping("/{key}")
    public ResponseEntity<String> addCache(@PathVariable String key,
                                           @RequestParam(value = "value") String value,
                                           @RequestParam(value = "expiresAt", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt) {
        String msg = cacheService.addCache(key, value, expiresAt);
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/{key}")
    public ResponseEntity<Cache> getCache(@PathVariable String key) {
        Cache cache = cacheService.getCache(key);
        return ResponseEntity.ok(cache);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteCache(
            @PathVariable String key) {
        String msg = cacheService.deleteCache(key);
        return ResponseEntity.ok(msg);
    }

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
    public ResponseEntity<Node> routeKey(
            @PathVariable String key
    ) {
        return ResponseEntity.ok(
                clusterManager.getOwner(key)
        );
    }

}
