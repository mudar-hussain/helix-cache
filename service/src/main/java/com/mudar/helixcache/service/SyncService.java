package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.transport.ClientNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {
    private final ClusterManager clusterManager;
    private final ClientNode clientNode;
    private final CacheService cacheService;

    public void syncToRecoveredNode(Node recoveredNode) {
        log.info("Starting anti-entropy sync for recovered node {}", recoveredNode.id());

        String localNodeId = clusterManager.getLocalNodeId();

        for(Node peer: clusterManager.getNodes()) {
            if(peer.id().equals(recoveredNode.id()) && (!clusterManager.containsNode(peer.id()))) continue;
            try {
                List<Cache> cacheListToSync;
                if(peer.id().equals(localNodeId)) {
                    cacheListToSync = cacheService.getCacheListForNode(recoveredNode.id());
                } else {
                    cacheListToSync = clientNode.fetchCacheListForNode(peer, recoveredNode.id());
                }
                log.info("Pushing {} key(s) from {} to recovered node {}", cacheListToSync.size(), peer.id(), recoveredNode.id());
                for(Cache cache: cacheListToSync) {
                    try {
                        clientNode.replicateCache(
                                recoveredNode,
                                cache.getKey(),
                                cache.getValue(),
                                cache.getExpiresAt() != null
                                        ? cache.getExpiresAt().toLocalDateTime() : null
                        );
                    } catch (Exception e) {
                        log.warn("Failed to sync key '{}' to {}: {}", cache.getKey(), recoveredNode.id(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch keys from peer {} for sync: {}", peer.id(), e.getMessage());
            }
        }
        log.info("Anti-entropy sync complete for node {}", recoveredNode.id());
    }
}
