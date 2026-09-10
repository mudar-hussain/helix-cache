package com.mudar.helixcache.Scheduler;

import com.mudar.helixcache.cluster.ClusterManager;
import com.mudar.helixcache.dto.Hint;
import com.mudar.helixcache.store.HintedHandOffStore;
import com.mudar.helixcache.transport.ClientNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HintedHandOffScheduler {
    private final HintedHandOffStore hintedHandOffStore;
    private final ClusterManager clusterManager;
    private final ClientNode clientNode;

    @Scheduled(fixedDelay = 10000)
    public void flushHints() {
        clusterManager.getNodes().forEach(node -> {
            if(node.id().equals(clusterManager.getLocalNodeId()) || !clusterManager.containsNode(node.id())) return;
            List<Hint> pendingHints = new ArrayList<>(hintedHandOffStore.getHintsForNode(node.id()));
            if(pendingHints.isEmpty()) return;
            log.info("Flushing {} hints to recovered node {}", pendingHints.size(), node.id());
            for(Hint hint: pendingHints) {
                try {
                    clientNode.replicateCache(node, hint.key(), hint.value(), hint.expiresAt());
                    hintedHandOffStore.removeHint(node.id(), hint);
                    log.info("Hint delivered to {}: key='{}'", node.id(), hint.key());
                } catch (Exception e) {
                    log.info("Hint delivered to {} failed for key='{}': {}", node.id(), hint.key(), e.getMessage()); //leave hint in queue for next retry
                }
            }
        });
    }

}
