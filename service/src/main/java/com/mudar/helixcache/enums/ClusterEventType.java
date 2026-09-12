package com.mudar.helixcache.enums;

public enum ClusterEventType {
    CACHE_PUT,
    CACHE_GET,
    CACHE_DELETE,
    CACHE_MISS,
    CACHE_EXPIRED,
    REPLICA_WRITE,
    REPLICA_FAILED,
    QUORUM_SUCCESS,
    QUORUM_FAILED,
    HINT_ENQUEUED,
    HING_DELIVERED,
    NODE_UP,
    NODE_SUSPECT,
    NODE_DOWN,
    SYNC_STARTED,
    SYNC_COMPLETE
}
