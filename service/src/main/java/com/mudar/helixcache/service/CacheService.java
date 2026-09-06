package com.mudar.helixcache.service;

import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.store.CacheStore;
import com.mudar.helixcache.utils.HelixConstant;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CacheService {
    private final CacheStore cacheStore;
    private final NodeProperties nodeProperties;

    public String addCache(String key, String value, Long ttl, LocalDateTime expiresAt) {
        if(expiresAt != null && ttl != null) {
            throw new HelixValidationException(HelixConstant.ERROR_TTL_EXPIRY_BOTH);
        }
        if(expiresAt != null) {
            return addCache(key, value, HelixUtils.convertToTimestamp(expiresAt));
        } else if(ttl != null){
            return addCacheWithTtl(key, value, ttl);
        } else {
            return addCache(key, value, null);
        }
    }

    public String addCacheWithTtl(String key, String value, Long ttl) {
        return addCache(key, value, HelixUtils.addSecondsToCurrentTimestamp(ttl));
    }

    public String addCache(String key, String value, Timestamp expiresAt) {
        HelixUtils.validateKeyValueExpiresAtForCreate(key, value, expiresAt);
        Timestamp createdAt = HelixUtils.getCurrentTimestamp();
        return cacheStore.put(key, new Cache(key, value, nodeProperties.getId(), createdAt, expiresAt));
    }

    public Cache getCache(String key) {
        validateExistKey(key);
        Cache cache = cacheStore.get(key);
        if(HelixUtils.isExpired(cache.getExpiresAt())) {
            cacheStore.remove(key);
            throw new HelixValidationException(HelixConstant.ERROR_KEY_EXPIRED);
        }
        cache.setLastAccessedAt(HelixUtils.getCurrentTimestamp());
        return cache;
    }

    public String deleteCache(String key) {
        validateExistKey(key);
        cacheStore.remove(key);
        return HelixConstant.SUCCESS_CACHE_REMOVED;
    }

    public int size() {
        return cacheStore.size();
    }

    public CacheStats getCacheStats() {
        return new CacheStats(size());
    }

    public void validateExistKey(String key) {
        HelixUtils.validateKey(key);
        if(!cacheStore.contains(key)) {
            throw new HelixValidationException(HelixConstant.ERROR_KEY_NOT_EXIST);
        }
    }

}
