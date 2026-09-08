package com.mudar.helixcache.store;

import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.utils.HelixConstant;
import com.mudar.helixcache.utils.HelixUtils;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class CacheStore {

    //ConcurrentMap uses lock striping or bucket-level locks, allowing multiple threads to read/write concurrently.
    //Iteration does not throw ConcurrentModificationException.
    private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public String put(String key, Cache cache) {
        HelixUtils.validateKeyCache(key, cache);
        if(this.cacheMap.put(key, cache) == null) {
            return HelixConstant.SUCCESS_CACHE_ADD;
        }

        return HelixConstant.SUCCESS_CACHE_UPDATE;
    }

    public Cache get(String key) {
        return cacheMap.get(key);
    }

    public Long getNodeVersion(String key) {
        HelixUtils.validateKey(key);
        if(cacheMap.containsKey(key)) {
            return cacheMap.get(key).getVersion();
        }
        return 0L;
    }

    public Cache remove(String key) {
        return cacheMap.remove(key);
    }

    public boolean contains(String key) {
        return cacheMap.containsKey(key);
    }

    public int size() {
        return cacheMap.size();
    }

}
