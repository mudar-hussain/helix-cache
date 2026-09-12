package com.mudar.helixcache.cluster;

import com.mudar.helixcache.enums.ClusterEventType;
import com.mudar.helixcache.model.ClusterEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClusterEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(ClusterEventType clusterEventType, String nodeId, String key, String detail, String severity) {
        applicationEventPublisher.publishEvent(new ClusterEvent(clusterEventType, nodeId, key, detail, severity));
    }

    //Convenience overload - no key (node-level events)
    public void publish(ClusterEventType clusterEventType, String nodeId, String detail, String severity) {
        publish(clusterEventType, nodeId, null, detail, severity);
    }
}
