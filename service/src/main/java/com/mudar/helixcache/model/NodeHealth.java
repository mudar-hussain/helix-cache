package com.mudar.helixcache.model;

import com.mudar.helixcache.enums.NodeStatus;
import com.mudar.helixcache.utils.HelixConstant;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class NodeHealth {
    private final Node node;
    private NodeStatus nodeStatus = NodeStatus.UP;
    private int missedHeartbeats;
    private Timestamp lastSeenAt;

    public NodeHealth (Node node) {
        this.node = node;
        this.missedHeartbeats = 0;
        this.lastSeenAt = HelixUtils.getCurrentTimestamp();
    }

    public void recordHit() {
        this.nodeStatus = NodeStatus.UP;
        this.missedHeartbeats = 0;
        this.lastSeenAt = HelixUtils.getCurrentTimestamp();
    }

    public void recordMiss() {
        this.missedHeartbeats++;
        if(this.missedHeartbeats >= HelixConstant.NODE_DOWN_THRESHOLD) {
            this.nodeStatus = NodeStatus.DOWN;
        } else if(this.missedHeartbeats >= HelixConstant.NODE_SUSPECT_THRESHOLD) {
            this.nodeStatus = NodeStatus.SUSPECT;
        }
    }

    public boolean isDown() {
        return this.nodeStatus == NodeStatus.DOWN;
    }
}
