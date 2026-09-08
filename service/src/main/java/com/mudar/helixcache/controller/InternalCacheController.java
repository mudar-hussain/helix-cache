package com.mudar.helixcache.controller;

import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/cache")
public class InternalCacheController {

    private final CacheService cacheService;

    @PutMapping("/{key}")
    public ResponseEntity<String> addCache(@PathVariable String key,
                                           @RequestParam("value") String value,
                                           @RequestParam(value = "expiresAt", required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt) {
        String msg = cacheService.writeCacheLocal(key, value, expiresAt);
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
}
