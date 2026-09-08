package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.store.CacheStore;
import com.mudar.helixcache.transport.ClientNode;
import com.mudar.helixcache.transport.LocalNode;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    private final CacheStore cacheStore;
    private final ClientNode clientNode;
    private final LocalNode localNode;
    private final ClusterManager clusterManager;

    public String addCache(String key, String value, LocalDateTime expiresAt) {
        HelixUtils.validateKey(key);
        List<Node> replicas = clusterManager.getReplicas(key);
        int writeQuorum = clusterManager.getClusterProperties().getWriteQuorum();
        String successMsg = "Cache entry written";
        int successCount = 0;
        List<String> failures = new ArrayList<>();
        for(Node replica: replicas) {
            try{
                String result;
                if(replica.id().equals(clusterManager.getLocalNode().id())) {
                    result = localNode.addCacheWithLocalDateTime(key, value, expiresAt);
                } else {
                    result = clientNode.replicateCache(replica, key, value, expiresAt);
                }
                successCount++;
                successMsg = result;
                log.info("Cache written to replica {}: {}", replica.id(), result);
            } catch (Exception e) {
                log.warn("Replication to {} failed for key '{}': {}", replica.id(), key, e.getMessage());
                failures.add(replica.id() + ": " + e.getMessage());
            }
            log.info("Write quorum for key '{}': {}/{} succeeded (required: {})", key, successCount, replicas.size(), writeQuorum);
            if(successCount<writeQuorum) {
                throw new HelixValidationException("Write quorum not met: " + successCount + "/" + replicas.size()
                                                + " replicas acknowledged. Failures: " + failures);
            }
        }
        return successMsg;
    }

    public String writeCacheLocal(String key, String value, LocalDateTime expiresAt) {
        return localNode.addCacheWithLocalDateTime(key, value, expiresAt);
    }

    public Cache getCache(String key) {
        HelixUtils.validateKey(key);
        Node owner = clusterManager.getOwner(key);
        if(owner.id().equals(clusterManager.getLocalNode().id())) {
            return localNode.getCache(key);
        }
        return clientNode.getCache(owner, key);
    }

    public String deleteCache(String key) {
        HelixUtils.validateKey(key);
        Node owner = clusterManager.getOwner(key);
        if(owner.id().equals(clusterManager.getLocalNode().id())) {
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
