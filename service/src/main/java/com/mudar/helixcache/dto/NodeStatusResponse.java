package com.mudar.helixcache.dto;

import com.mudar.helixcache.enums.NodeStatus;

import java.sql.Timestamp;

public record NodeStatusResponse (
        String nodeId,
        String address,
        NodeStatus nodeStatus,
        boolean isLocal,
        int keyCount,
        int missedHeartbeats,
        Timestamp lastSeenAt
) {}
