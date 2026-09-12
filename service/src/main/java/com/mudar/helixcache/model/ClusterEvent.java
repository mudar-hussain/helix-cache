package com.mudar.helixcache.model;

import com.mudar.helixcache.enums.ClusterEventType;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.sql.Timestamp;

@Getter
public class ClusterEvent extends ApplicationEvent {
    private final ClusterEventType clusterEventType;
    private final String nodeId;
    private final String key;           // nullable - not all events have a key
    private final String detail;
    private final String severity;      //INFO, WARN, ERROR
    private final Timestamp eventTimestamp;

    public ClusterEvent(ClusterEventType clusterEventType, String nodeId, String key, String detail, String severity) {
        super(clusterEventType);
        this.clusterEventType = clusterEventType;
        this.nodeId = nodeId;
        this.key = key;
        this.detail = detail;
        this.severity = severity;
        this.eventTimestamp = HelixUtils.getCurrentTimestamp();
    }
}
