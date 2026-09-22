
import { ClusterEventType } from "../enums/helix.enum"


export const LOG_CATEGORIES: Record<string, ClusterEventType[]> = {
    'ALL': [],
    'NODE': [ClusterEventType.HEARTBEAT, ClusterEventType.NODE_UP, ClusterEventType.NODE_DOWN, ClusterEventType.NODE_SUSPECT, ClusterEventType.PARTITION_SET, ClusterEventType.PARTITION_HEALED],
    'QUORUM': [ClusterEventType.QUORUM_SUCCESS, ClusterEventType.QUORUM_FAILED],
    'REPLICA': [ClusterEventType.REPLICA_WRITE, ClusterEventType.REPLICA_FAILED],
    'SYNC': [ClusterEventType.SYNC_STARTED, ClusterEventType.SYNC_COMPLETE],
    'HINT': [ClusterEventType.HINT_ENQUEUED, ClusterEventType.HINT_DELIVERED],
    'CACHE': [ClusterEventType.CACHE_PUT, ClusterEventType.CACHE_GET, ClusterEventType.CACHE_DELETE, ClusterEventType.CACHE_MISS, ClusterEventType.CACHE_EXPIRED]
};

// Badge background colour keyed by event type
export const BADGE_STYLE: Partial<Record<ClusterEventType, string>> = {
    [ClusterEventType.HEARTBEAT]: 'background:var(--color-success-dim);color:var(--color-success);border:1px solid var(--color-success-border)',
    [ClusterEventType.NODE_UP]: 'background:var(--color-success-dim);color:var(--color-success);border:1px solid var(--color-success-border)',
    [ClusterEventType.NODE_SUSPECT]: 'background:var(--color-warning-dim);color:var(--color-warning);border:1px solid var(--color-warning-border)',
    [ClusterEventType.NODE_DOWN]: 'background:var(--color-danger-dim);color:var(--color-danger);border:1px solid var(--color-danger-border)',
    [ClusterEventType.REPLICA_WRITE]: 'background:var(--color-accent-dim);color:var(--color-accent);border:1px solid var(--color-accent-border)',
    [ClusterEventType.REPLICA_FAILED]: 'background:var(--color-danger-dim);color:var(--color-danger);border:1px solid var(--color-danger-border)',
    [ClusterEventType.QUORUM_SUCCESS]: 'background:var(--color-accent-dim);color:var(--color-accent);border:1px solid var(--color-accent-border)',
    [ClusterEventType.QUORUM_FAILED]: 'background:var(--color-danger-dim);color:var(--color-danger);border:1px solid var(--color-danger-border)',
    [ClusterEventType.SYNC_STARTED]: 'background:var(--color-accent-dim);color:var(--color-accent);border:1px solid var(--color-accent-border)',
    [ClusterEventType.SYNC_COMPLETE]: 'background:var(--color-info-dim);color:var(--color-info);border:1px solid var(--color-info-border)',
    [ClusterEventType.CACHE_GET]: 'background:var(--color-info-dim);color:var(--color-info);border:1px solid var(--color-info-border)',
    [ClusterEventType.CACHE_PUT]: 'background:var(--color-info-dim);color:var(--color-info);border:1px solid var(--color-info-border)',
    [ClusterEventType.CACHE_MISS]: 'background:var(--color-warning-dim);color:var(--color-warning);border:1px solid var(--color-warning-border)',
    [ClusterEventType.CACHE_DELETE]: 'background:var(--color-danger-dim);color:var(--color-danger);border:1px solid var(--color-danger-border)',
    [ClusterEventType.CACHE_EXPIRED]: 'background:var(--color-danger-dim);color:var(--color-danger);border:1px solid var(--color-danger-border)',
    [ClusterEventType.HINT_ENQUEUED]: 'background:var(--color-info-dim);color:var(--color-info);border:1px solid var(--color-info-border)',
    [ClusterEventType.HINT_DELIVERED]: 'background:var(--color-info-dim);color:var(--color-info);border:1px solid var(--color-info-border)',
    [ClusterEventType.CONFLICT_DETECTED]: 'background:var(--color-warning-dim);color:var(--color-warning);border:1px solid var(--color-warning-border)',
    [ClusterEventType.PARTITION_SET]: 'background:var(--color-warning-dim);color:var(--color-warning);border:1px solid var(--color-warning-border)',
    [ClusterEventType.PARTITION_HEALED]: 'background:var(--color-success-dim);color:var(--color-success);border:1px solid var(--color-success-border)',
};

export const MERGEABLE_TYPES = new Set<ClusterEventType>([
    ClusterEventType.HEARTBEAT,
    ClusterEventType.NODE_UP,
    ClusterEventType.NODE_SUSPECT,
    ClusterEventType.NODE_DOWN,
])