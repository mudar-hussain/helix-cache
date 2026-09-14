package com.mudar.helixcache.controller;

import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @PutMapping("/{key}")
    public ResponseEntity<Cache> addCache(@PathVariable String key,
                                           @RequestParam(value = "value") String value,
                                           @RequestParam(value = "ttlSeconds", required = false) Long ttlSeconds) {
        return ResponseEntity.ok(cacheService.addCache(key, value, ttlSeconds));
    }

    @GetMapping("/{key}")
    public ResponseEntity<Cache> getCache(@PathVariable String key) {
        return ResponseEntity.ok(cacheService.getCache(key));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> deleteCache(@PathVariable String key) {
        return ResponseEntity.ok(cacheService.deleteCache(key));
    }

}
