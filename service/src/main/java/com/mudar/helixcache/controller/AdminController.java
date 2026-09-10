package com.mudar.helixcache.controller;

import com.mudar.helixcache.cluster.NodeStateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final NodeStateManager nodeStateManager;

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
}
