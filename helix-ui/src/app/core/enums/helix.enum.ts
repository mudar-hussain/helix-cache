//mirroring Java Enums as it as here

export enum NodeStatus {
    UP = 'UP',
    SUSPECT = 'SUSPECT',
    DOWN = 'DOWN'
}

export enum Severity {
    INFO = 'INFO',
    WARN = 'WARN',
    ERROR = 'ERROR'
}

export enum ClusterEventType { 
    CACHE_PUT = 'CACHE_PUT',
    CACHE_GET = 'CACHE_GET',
    CACHE_DELETE = 'CACHE_DELETE',
    CACHE_MISS = 'CACHE_MISS',
    CACHE_EXPIRED = 'CACHE_EXPIRED',
    REPLICA_WRITE = 'REPLICA_WRITE',
    REPLICA_FAILED = 'REPLICA_FAILED',
    QUORUM_SUCCESS = 'QUORUM_SUCCESS',
    QUORUM_FAILED = 'QUORUM_FAILED',
    HINT_ENQUEUED = 'HINT_ENQUEUED',
    HINT_DELIVERED = 'HINT_DELIVERED',
    NODE_UP = 'NODE_UP',
    NODE_SUSPECT = 'NODE_SUSPECT',
    NODE_DOWN = 'NODE_DOWN',
    SYNC_STARTED = 'SYNC_STARTED',
    SYNC_COMPLETE = 'SYNC_COMPLETE'
 };
export enum TtlOption {
    'never' = -1, '10s' = 10, '30s' = 30, '2m' = 120, '5m' = 300, '10m' = 600
}
 
