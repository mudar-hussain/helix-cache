package com.mudar.helixcache.controller;

import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
                                                  @RequestParam("expiresAt") LocalDateTime expiresAt) {
        String msg = cacheService.addCache(key, value, expiresAt);
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
}
