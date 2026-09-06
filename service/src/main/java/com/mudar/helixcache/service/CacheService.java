package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.cluster.Node;
import com.mudar.helixcache.config.NodeProperties;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.store.CacheStore;
import com.mudar.helixcache.transport.ClientNode;
import com.mudar.helixcache.transport.LocalNode;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class CacheService {
    private final CacheStore cacheStore;
    private final NodeProperties nodeProperties;
    private final ClientNode clientNode;
    private final LocalNode localNode;
    private final ClusterManager clusterManager;

    public String addCache(String key, String value, LocalDateTime expiresAt) {
        HelixUtils.validateKey(key);
        Node owner = clusterManager.getOwner(key);
        if(owner.id().equals(nodeProperties.getId())) {
            return localNode.addCacheWithLocalDateTime(key, value, expiresAt);
        }
        return clientNode.addCache(owner, key, value, expiresAt);
    }

    public Cache getCache(String key) {
        HelixUtils.validateKey(key);
        Node owner = clusterManager.getOwner(key);
        if(owner.id().equals(nodeProperties.getId())) {
            return localNode.getCache(key);
        }
        return clientNode.getCache(owner, key);
    }

    public String deleteCache(String key) {
        HelixUtils.validateKey(key);
        Node owner = clusterManager.getOwner(key);
        if(owner.id().equals(nodeProperties.getId())) {
            return localNode.deleteCache(key);
        }
        return clientNode.deleteCache(owner, key);
    }

    public int size() {
        return cacheStore.size();
    }

    public CacheStats getCacheStats() {
        return new CacheStats(size());
    }

}
