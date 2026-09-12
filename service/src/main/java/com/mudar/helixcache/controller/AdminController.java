package com.mudar.helixcache.controller;

import com.mudar.helixcache.Scheduler.HotKeyPredictor;
import com.mudar.helixcache.cluster.NodeStateManager;
import com.mudar.helixcache.dto.HotKeyPredictionResponse;
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
}
