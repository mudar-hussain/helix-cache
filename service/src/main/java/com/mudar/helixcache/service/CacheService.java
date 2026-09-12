package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.dto.CacheStats;
import com.mudar.helixcache.dto.Hint;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.store.CacheStore;
import com.mudar.helixcache.store.HintedHandOffStore;
import com.mudar.helixcache.transport.ClientNode;
import com.mudar.helixcache.transport.LocalNode;
import com.mudar.helixcache.utils.HelixConstant;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    private final CacheStore cacheStore;
    private final ClientNode clientNode;
    private final LocalNode localNode;
    private final ClusterManager clusterManager;
    private final HintedHandOffStore hintedHandOffStore;

    public String addCache(String key, String value, LocalDateTime expiresAt) {
        HelixUtils.validateKey(key);
        List<Node> replicas = clusterManager.getReplicas(key);
        int writeQuorum = clusterManager.getWriteQuorum();
        String successMsg = "Cache entry written";
        int successCount = 0;
        List<String> failures = new ArrayList<>();
        List<Hint> pendingHints = new ArrayList<>();
        for(Node replica: replicas) {
            try{
                String result;
                if(replica.id().equals(clusterManager.getLocalNodeId())) {
                    result = localNode.addCacheWithLocalDateTime(key, value, expiresAt);
                } else {
                    result = clientNode.replicateCache(replica, key, value, expiresAt);
                }
                successCount++;
                successMsg = result;
                log.info("Cache written to replica {}: {}", replica.id(), result);
            } catch (Exception e) {
                log.warn("Replication to {} failed for key '{}': {} - storing hint", replica.id(), key, e.getMessage());
                failures.add(replica.id() + ": " + e.getMessage());
                pendingHints.add(new Hint(key, value, expiresAt, replica.id(), replica.address(), HelixUtils.getCurrentTimestamp().toLocalDateTime()));
            }
        }
        log.info("Write quorum for key '{}': {}/{} succeeded (required: {})", key, successCount, replicas.size(), writeQuorum);
        if(successCount>=writeQuorum) {
            pendingHints.forEach(hintedHandOffStore::add);
        } else {
            throw new HelixValidationException("Write quorum not met: " + successCount + "/" + replicas.size()
                    + " replicas acknowledged. Failures: " + failures);
        }
        return successMsg;
    }

    public String writeCacheLocal(String key, String value, LocalDateTime expiresAt) {
        return localNode.addCacheWithLocalDateTime(key, value, expiresAt);
    }

    public Cache getCache(String key) {
        HelixUtils.validateKey(key);
        List<Node> replicas = clusterManager.getReplicas(key);
        int readQuorum = clusterManager.getReadQuorum();

        List<Cache> responses = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for(Node replica: replicas) {
            try {
                Cache result;
                if(replica.id().equals(clusterManager.getLocalNodeId())) {
                    result = localNode.getCache(key);
                } else {
                    result = clientNode.getCacheFromReplica(replica, key);
                }
                responses.add(result);
                log.info("Read from replica {}: version={}", replica.id(), result.getVersion());
            } catch (Exception e) {
                log.warn("Read from replica {} failed for key '{}': {}", replica.id(), key, e.getMessage());
                failures.add(replica.id() + ": " + e.getMessage());
            }
        }
        if(responses.size() < readQuorum) {
            if (responses.isEmpty()) {
                throw new HelixValidationException(
                        failures.isEmpty() ? "No Replicas Available" : failures.get(0).split(": ", 2)[1]
                );
            }
            throw new HelixValidationException(
                    "Read quorum not met: only " + responses.size() + "/" + replicas.size()
                    + " replicas responded (required: " + readQuorum + ")"
            );
        }

        return responses.stream()
                .max(Comparator.comparingLong(Cache::getVersion))
                .orElseThrow(() -> new HelixValidationException(HelixConstant.ERROR_KEY_NOT_EXIST));
    }

    public Cache readCacheLocal(String key) {
        return localNode.getCache(key);
    }

    public String deleteCache(String key) {
        HelixUtils.validateKey(key);
        List<Node> replicas = clusterManager.getReplicas(key);
        int writeQuorum = clusterManager.getWriteQuorum();

        String successMsg = "Cache entry removed";
        int successCount = 0;
        List<String> failures = new ArrayList<>();

        for(Node replica: replicas) {
            try{
                String result;
                if(replica.id().equals(clusterManager.getLocalNodeId())) {
                    result = localNode.deleteCache(key);
                } else {
                    result = clientNode.deleteCacheFromReplica(replica, key);
                }
                successCount++;
                successMsg = result;
                log.info("Deleted key '{}' from replica {}: {}", key, replica.id(), result);
            } catch (HelixValidationException e) {
                // If replica says key doesn't exist, the desired state is achieved - count as success
                if(HelixConstant.ERROR_KEY_NOT_EXIST.equals(e.getMessage())) {
                    successCount++;
                    log.info("Key '{}' already absent on replicas {} - counting on success", key, replica.id());
                } else {
                    log.warn("Deletion from replica {} failed for key '{}': {}", replica.id(), key, e.getMessage());
                    failures.add(replica.id() + ": " + e.getMessage());
                }
            } catch (Exception e) {
                log.warn("Deletion from replica {} failed for key '{}': {}", replica.id(), key, e.getMessage());
                failures.add(replica.id() + ": " + e.getMessage());
            }
        }
        log.info("Delete quorum for key '{}': {}/{} succeeded (required: {})", key, successCount, replicas.size(), writeQuorum);
        if(successCount < writeQuorum) {
            throw new HelixValidationException("Delete quorum not met: " + successCount + "/" + replicas.size()
                    + " replicas acknowledged. Failures: " + failures);
        }
        return successMsg;
    }

    public String deleteCacheLocal(String key) {
        return localNode.deleteCache(key);
    }

    public int size() {
        return cacheStore.size();
    }

    public CacheStats getCacheStats() {
        return new CacheStats(size());
    }

    public List<Cache> getCacheListForNode(String targetNodeId) {
        return cacheStore.getAll().stream()
                .filter(cache -> clusterManager.getReplicas(cache.getKey())
                        .stream()
                        .anyMatch(node -> node.id().equals(targetNodeId)))
                .toList();
    }

}
