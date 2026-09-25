package com.mudar.helixcache.controller;

import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.service.CacheService;
import com.mudar.helixcache.store.HintedHandOffStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/cache")
public class InternalCacheController {

    private final CacheService cacheService;
    private final HintedHandOffStore hintedHandOffStore;

    @PutMapping("/{key}")
    public ResponseEntity<Cache> addCache(@PathVariable String key,
                                           @RequestParam("value") String value,
                                           @RequestParam(value = "ttlSeconds", required = false) Long ttlSeconds) {
        return ResponseEntity.ok(cacheService.writeCacheLocal(key, value, ttlSeconds));
    }

    @GetMapping("/{key}")
    public ResponseEntity<Cache> getCache(@PathVariable String key) {
        Cache cache = cacheService.readCacheLocal(key);
        return ResponseEntity.ok(cache);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteCache(@PathVariable String key) {
        String msg = cacheService.deleteCacheLocal(key);
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/hints")
    public ResponseEntity<Map<String, Integer>> getHintQueueDepth() {
        return ResponseEntity.ok(hintedHandOffStore.getQueueDepths());
    }

    @GetMapping("/sync/node")
    public ResponseEntity<List<Cache>> getCacheListForNode(@RequestParam String targetNodeId) {
        return ResponseEntity.ok(cacheService.getCacheListForNode(targetNodeId));
    }

    @GetMapping("/fetch/keys")
    public ResponseEntity<Set<String>> getLocalKeys() {
        return ResponseEntity.ok(cacheService.getLocalKeys());
    }
}
