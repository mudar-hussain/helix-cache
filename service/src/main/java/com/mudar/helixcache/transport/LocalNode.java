package com.mudar.helixcache.transport;

import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.store.CacheStore;
import com.mudar.helixcache.utils.HelixConstant;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LocalNode {
    private final CacheStore cacheStore;
    private final NodeProperties nodeProperties;


    public String addCacheWithLocalDateTime(String key, String value, LocalDateTime expiresAt) {
        if(expiresAt != null) {
            return addCache(key, value, HelixUtils.convertToTimestamp(expiresAt));
        } else {
            return addCache(key, value, null);
        }
    }

    public String addCache(String key, String value, Timestamp expiresAt) {
        HelixUtils.validateKeyValueExpiresAtForCreate(key, value, expiresAt);
        Timestamp createdAt = HelixUtils.getCurrentTimestamp();
        return cacheStore.put(key, new Cache(key, value, nodeProperties.getId(), createdAt, expiresAt));
    }

    public Cache getCache(String key) {
        validateExistKey(key);
        Cache cache = cacheStore.get(key);
        if(cache==null) {
            throw new HelixValidationException(HelixConstant.ERROR_KEY_NOT_EXIST);
        }
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
