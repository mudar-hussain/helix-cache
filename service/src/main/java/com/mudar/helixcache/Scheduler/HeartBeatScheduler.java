package com.mudar.helixcache.Scheduler;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.service.NodeHealthService;
import com.mudar.helixcache.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartBeatScheduler {
    private final ClusterManager clusterManager;
    private final NodeHealthService nodeHealthService;
    private final SyncService syncService;

    @Scheduled(fixedDelay = 5000)
    public void sendHeartBeats() {
        String localNodeId = clusterManager.getLocalNodeId();
        List<Node> nodes = clusterManager.getNodes();
        for(Node peer: nodes) {
            if(peer.id().equals(localNodeId)) continue;

            try {
                nodeHealthService.pingNode(peer.address());
                nodeHealthService.recordHit(peer.id());

                //If node was Down and is now responding, add it back to the ring
                if (!clusterManager.containsNode(peer.id())) {
                    log.info("Node {} recovered: adding back to ring", peer.id());
                    syncService.syncToRecoveredNode(peer);
                    clusterManager.addNode(peer);
                    log.info("Node {} re-added to ring after sync", peer.id());
                }
            } catch (Exception e) {
                nodeHealthService.recordMiss(peer.id());
                log.warn("Heartbeat to node {} failed: {}", peer.id(), e.getMessage());

                //If node is now declared Down, remove it from the ring
                if(nodeHealthService.isDown(peer.id()) && clusterManager.containsNode(peer.id())) {
                    log.error("Node {} declared DOWN - removing from ring", peer.id());
                    clusterManager.removeNode(peer.id());
                }
            }
        }

    }
}
