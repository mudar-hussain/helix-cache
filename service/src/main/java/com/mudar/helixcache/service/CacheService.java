package com.mudar.helixcache.service;

import com.mudar.helixcache.cluster.ClusterEventPublisher;
import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.dto.*;
import com.mudar.helixcache.enums.ClusterEventType;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.store.AccessTracker;
import com.mudar.helixcache.store.CacheStore;
import com.mudar.helixcache.store.HintedHandOffStore;
import com.mudar.helixcache.transport.ClientNode;
import com.mudar.helixcache.transport.LocalNode;
import com.mudar.helixcache.utils.HelixConstant;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    private final CacheStore cacheStore;
    private final ClientNode clientNode;
    private final LocalNode localNode;
    private final ClusterManager clusterManager;
    private final HintedHandOffStore hintedHandOffStore;
    private final ClusterEventPublisher clusterEventPublisher;
    private final AccessTracker accessTracker;
    private final NodeHealthService nodeHealthService;

    public Cache addCache(String key, String value, Long ttlSeconds) {
        HelixUtils.validateKey(key);
        List<Node> replicas = clusterManager.getReplicas(key);
        int writeQuorum = clusterManager.getWriteQuorum();
        Cache cache = null;
        int successCount = 0;
        List<String> failures = new ArrayList<>();
        List<Hint> pendingHints = new ArrayList<>();
        String primaryNode = replicas.get(0).id();
        for(Node replica: replicas) {
            try{
                Cache temp;
                if(replica.id().equals(clusterManager.getLocalNodeId())) {
                    temp = localNode.addCache(key, value, ttlSeconds, primaryNode);
                } else {
                    temp = clientNode.replicateCache(replica, key, value, ttlSeconds);
                }
                successCount++;
                accessTracker.record(key);
                clusterEventPublisher.publish(ClusterEventType.REPLICA_WRITE, replica.id(), key,
                        "Key replicated to " + replica.id(), "INFO");
                cache = temp;
                log.info("Cache with key '{}' written to replica {}", cache.getKey(), replica.id());
            } catch (Exception e) {
                log.warn("Replication to {} failed for key '{}': {} - storing hint", replica.id(), key, e.getMessage());
                failures.add(replica.id() + ": " + e.getMessage());
                pendingHints.add(new Hint(key, value, replica.id(), replica.address(), HelixUtils.getCurrentTimestamp().toLocalDateTime(), ttlSeconds));
                clusterEventPublisher.publish(ClusterEventType.HINT_ENQUEUED, replica.id(), key,
                        "Hint stored for offline node " + replica.id(), "WARN");
                clusterEventPublisher.publish(ClusterEventType.REPLICA_FAILED, replica.id(), key,
                        "Key replication failed: " + e.getMessage(), "WARN");
            }
        }
        log.info("Write quorum for key '{}': {}/{} succeeded (required: {})", key, successCount, replicas.size(), writeQuorum);
        if(successCount>=writeQuorum) {
            pendingHints.forEach(hintedHandOffStore::add);
            clusterEventPublisher.publish(ClusterEventType.CACHE_PUT, clusterManager.getLocalNodeId(), key,
                    "Key written successfully", "INFO");
            clusterEventPublisher.publish(ClusterEventType.QUORUM_SUCCESS, clusterManager.getLocalNodeId(), key,
                    "Write quorum met: " + successCount + "/" + replicas.size(), "INFO");
        } else {
            clusterEventPublisher.publish(ClusterEventType.QUORUM_FAILED, clusterManager.getLocalNodeId(), key,
                    "Write quorum not met: " + successCount + "/" + replicas.size(), "ERROR");
            throw new HelixValidationException("Write quorum not met: " + successCount + "/" + replicas.size()
                    + " replicas acknowledged. Failures: " + failures);
        }
        return cache;
    }

    public Cache writeCacheLocal(String key, String value, Long ttlSeconds) {
        List<Node> replicas = clusterManager.getReplicas(key);
        String primaryNode = replicas.get(0).id();
        return localNode.addCache(key, value, ttlSeconds, primaryNode);
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
                accessTracker.record(key);
                log.info("Read from replica {}: version={}", replica.id(), result.getVersion());
            } catch (Exception e) {
                log.warn("Read from replica {} failed for key '{}': {}", replica.id(), key, e.getMessage());
                failures.add(replica.id() + ": " + e.getMessage());
            }
        }
        if(responses.size() < readQuorum) {
            if (responses.isEmpty()) {
                clusterEventPublisher.publish(ClusterEventType.CACHE_MISS, clusterManager.getLocalNodeId(), key,
                        "Key not found on any replica", "WARN");
                throw new HelixValidationException(
                        failures.isEmpty() ? "No Replicas Available" : failures.get(0).split(": ", 2)[1]
                );
            }
            throw new HelixValidationException(
                    "Read quorum not met: only " + responses.size() + "/" + replicas.size()
                    + " replicas responded (required: " + readQuorum + ")"
            );
        }
        Map<String, List<Cache>> byValue = responses.stream()
                .collect(Collectors.groupingBy(Cache::getValue));

        if(byValue.size() > 1) {
            //Geuine conflict - same version, different values
            clusterEventPublisher.publish(ClusterEventType.CONFLICT_DETECTED, clusterManager.getLocalNodeId(), key,
                    "Conflict: " + byValue.size() + " divergent values detected", "WARN");
        }

        Cache result =  responses.stream()
                .max(Comparator.comparingLong(Cache::getVersion))
                .orElseThrow(() -> new HelixValidationException(HelixConstant.ERROR_KEY_NOT_EXIST));
        clusterEventPublisher.publish(ClusterEventType.CACHE_GET, clusterManager.getLocalNodeId(), key,
                "Key read successfully", "INFO");
        return result;
    }

    public Cache readCacheLocal(String key) {
        return localNode.getCache(key);
    }

    public String deleteCache(String key) {
        HelixUtils.validateKey(key);
        List<Node> replicas = clusterManager.getReplicas(key);
        int writeQuorum = clusterManager.getWriteQuorum();

        String successMsg = HelixConstant.SUCCESS_CACHE_REMOVED;
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

        clusterEventPublisher.publish(ClusterEventType.CACHE_DELETE, clusterManager.getLocalNodeId(), key,
                "Key deleted: " + successCount + "/" + replicas.size() + " replicas", "INFO");
        return successMsg;
    }

    public String deleteCacheLocal(String key) {
        return localNode.deleteCache(key);
    }

    public int size() {
        return cacheStore.size();
    }

    public ClusterStats getLocalClusterStats() {
        return new ClusterStats(
                cacheStore.size(),
                clusterManager.getReplicationFactor(),
                clusterManager.getWriteQuorum(),
                clusterManager.getReadQuorum(),
                clusterManager.getVirtualNodesPerNode()
        );
    }

    public ClusterStats getGlobalClusterStats() {
        List<Node> allNodes = clusterManager.getNodes();
        String localNodeId = clusterManager.getLocalNodeId();
        int totalRawKeys = 0;

        for(Node node: allNodes) {
            if(node.id().equals(localNodeId)) {
                totalRawKeys += cacheStore.size();
            } else {
                int remote = nodeHealthService.getRemoteKeyCount(node);
                if(remote >=0 ) totalRawKeys += remote;
            }
        }

        int uniqueKeys = (int) Math.round((double) totalRawKeys / clusterManager.getReplicationFactor());
        return new ClusterStats(
                uniqueKeys,
                clusterManager.getReplicationFactor(),
                clusterManager.getWriteQuorum(),
                clusterManager.getReadQuorum(),
                clusterManager.getVirtualNodesPerNode()
        );
    }

    public List<Cache> getCacheListForNode(String targetNodeId) {
        return cacheStore.getAll().stream()
                .filter(cache -> clusterManager.getReplicas(cache.getKey())
                        .stream()
                        .anyMatch(node -> node.id().equals(targetNodeId)))
                .toList();
    }

    public BulkSeedResult seedCache(int count) {
        return seedCache(count, "K");
    }

    public BulkSeedResult seedCache(BulkSeedRequest request) {
        return seedCache(request.count(), request.prefix() != null ? request.prefix() : "K");
    }

    public BulkSeedResult seedCache(int count, String prefix) {
        if(count<=0 || count > 50) {
            throw new HelixValidationException("Seed count must be between 1 and 50");
        }
        int succeeded = 0;
        for(int i = 1; i<=count; i++) {
            String key = prefix + i;
            String value = "seed:value:" + i;
            try {
                addCache(key, value, null);
                succeeded++;
            } catch (Exception e) {
                log.warn("Seed failed for key '{}': {}", key, e.getMessage());
            }
        }
        return new BulkSeedResult(count, succeeded, count-succeeded);
    }

}
