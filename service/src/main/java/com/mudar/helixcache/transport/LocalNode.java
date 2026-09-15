package com.mudar.helixcache.transport;

import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.store.CacheStore;
import com.mudar.helixcache.utils.HelixConstant;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class LocalNode {
    private final CacheStore cacheStore;
    private final NodeProperties nodeProperties;

    public Cache addCache(String key, String value, Long ttlSeconds, String primaryNode) {
        HelixUtils.validateKeyValue(key, value);
        Cache cache;
        if(cacheStore.contains(key)) {
            cache = cacheStore.get(key);
            cache.setValue(value);
            cache.setTtlSeconds(ttlSeconds);
            cache.setVersion(cache.getVersion()+1);
        } else {
            Timestamp createdAt = HelixUtils.getCurrentTimestamp();
            cache = new Cache(key, value, primaryNode, createdAt, ttlSeconds);
        }
        cacheStore.put(key, cache);
        return cache;
    }

    public Cache getCache(String key) {
        validateExistKey(key);
        Cache cache = cacheStore.get(key);
        if(cache==null) {
            cacheStore.recordMiss();
            throw new HelixValidationException(HelixConstant.ERROR_KEY_NOT_EXIST);
        }
        if(cache.getTtlSeconds() != null && cache.getTtlSeconds()>0 && HelixUtils.isExpired(HelixUtils.addSeconds(cache.getCreatedAt(), cache.getTtlSeconds()))) {
            cacheStore.remove(key);
            cacheStore.recordMiss();
            throw new HelixValidationException(HelixConstant.ERROR_KEY_EXPIRED);
        }
        cacheStore.recordHit();
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

    public void validateExistKey(String key) {
        HelixUtils.validateKey(key);
        if(!cacheStore.contains(key)) {
            throw new HelixValidationException(HelixConstant.ERROR_KEY_NOT_EXIST);
        }
    }
}
