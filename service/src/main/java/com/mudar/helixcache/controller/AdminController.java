package com.mudar.helixcache.controller;

import com.mudar.helixcache.scheduler.HotKeyPredictor;
import com.mudar.helixcache.cluster.NodeStateManager;
import com.mudar.helixcache.dto.HotKeyPredictionResponse;
import com.mudar.helixcache.service.ClusterService;
import com.mudar.helixcache.store.AccessTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final NodeStateManager nodeStateManager;
    private final AccessTracker accessTracker;
    private final HotKeyPredictor hotKeyPredictor;
    private final ClusterService clusterService;

    @PutMapping("/node/pause")
    public ResponseEntity<Map<String, String>> pause() {
        nodeStateManager.pause();
        return ResponseEntity.ok(Map.of("reason", "Node paused - ping will return 503"));
    }

    @PutMapping("/node/resume")
    public ResponseEntity<Map<String, String>> resume() {
        nodeStateManager.resume();
        return ResponseEntity.ok(Map.of("reason", "Node resumed - ping will return 200"));
    }

    @PostMapping("/node/slow")
    public ResponseEntity<Map<String, String>> slow(@RequestParam(defaultValue = "3000") long delayMs) {
        nodeStateManager.slow(delayMs);
        return ResponseEntity.ok(Map.of(
                "reason", "Node set to slow mode",
                "delay", String.valueOf(delayMs)
        ));
    }

    @GetMapping("/stats/access")
    public ResponseEntity<List<HotKeyPredictionResponse>> getAccessStats() {
        List<HotKeyPredictionResponse> stats = new ArrayList<>();
        for(String key: accessTracker.getTrackedKeys()) {
            stats.add(new HotKeyPredictionResponse(key, hotKeyPredictor.getEmaScoreForKey(key),
                    accessTracker.getRecentRate(key), accessTracker.getTotalAccess(key), false));
        }
        stats.sort((a,b) -> Double.compare(b.recentRate(), a.recentRate()));

        return ResponseEntity.ok(stats);

    }

    @GetMapping("/stats/predictions")
    public ResponseEntity<List<HotKeyPredictionResponse>> getPredictions() {
        return ResponseEntity.ok(hotKeyPredictor.getLastPredictions());
    }

    @PostMapping("/node/partition")
    public ResponseEntity<Map<String, Object>> setPartition(@RequestBody Map<String, List<String>> partitionMap) {
        clusterService.setPartition(partitionMap);
        return ResponseEntity.ok(Map.of("nodeId", clusterService.getLocalNodeId(),
                "blockedPeers", nodeStateManager.getBlockedPeers()));
    }

    @GetMapping("/node/partition")
    public ResponseEntity<Map<String, Object>> getPartition() {
        return ResponseEntity.ok(Map.of("nodeId", clusterService.getLocalNodeId(),
                "blockedPeers", nodeStateManager.getBlockedPeers()));
    }

    @PutMapping("/node/heal")
    public ResponseEntity<?> healPartition() {
        nodeStateManager.unblockAllPeer();
        return ResponseEntity.ok(Map.of("nodeId", clusterService.getLocalNodeId(),
                "blockedPeers", nodeStateManager.getBlockedPeers()));
    }
}
