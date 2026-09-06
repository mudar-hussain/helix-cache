package com.mudar.helixcache.controller;

import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.dto.NodeInfoResponse;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.service.CacheService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/cache")
@AllArgsConstructor
public class CacheController {

    private final CacheService cacheService;
    private final NodeProperties nodeProperties;

    @PutMapping("/{key}")
    public ResponseEntity<String> addCache(@PathVariable String key,
                                           @RequestParam(value = "value") String value,
                                           @RequestParam(value = "ttl", required = false) Long ttl,
                                           @RequestParam(value = "expiresAt", required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt) {
        log.info("PUT cache request: key={}, ttl={}, expiresAt={}",
                key, ttl, expiresAt);
        String msg = cacheService.addCache(key, value, ttl, expiresAt);
        return ResponseEntity.ok(msg);
    }

    @GetMapping("/{key}")
    public ResponseEntity<Cache> getCache(@PathVariable String key) {
        Cache cache = cacheService.getCache(key);
        if (cache == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .build();
        }
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

}
