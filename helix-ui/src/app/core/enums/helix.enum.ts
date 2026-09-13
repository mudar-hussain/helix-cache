//mirroring Java Enums as it as here
export enum NodeStatus { UP, SUSPECT, DOWN };
export enum Severity { INFO, WARN, ERROR};
export enum ClusterEventType { 
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
    HINT_DELIVERED,
    NODE_UP,
    NODE_SUSPECT,
    NODE_DOWN,
    SYNC_STARTED,
    SYNC_COMPLETE
 };
export enum TtlOption {
    never, '10s', '30s', '2m', '5m'
}
 
