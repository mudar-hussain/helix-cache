
import { ClusterEventType } from "../enums/helix.enum"


export const LOG_CATEGORIES: Record<string, ClusterEventType[]> = {
    'ALL': [],
    'NODE': [ClusterEventType.NODE_UP, ClusterEventType.NODE_DOWN, ClusterEventType.NODE_SUSPECT],
    'QUORUM': [ClusterEventType.QUORUM_SUCCESS, ClusterEventType.QUORUM_FAILED],
    'REPLICA': [ClusterEventType.REPLICA_WRITE, ClusterEventType.REPLICA_FAILED],
    'SYNC': [ClusterEventType.SYNC_STARTED, ClusterEventType.SYNC_COMPLETE],
    'HOT KEY': [ClusterEventType.CACHE_GET],
    'HINT': [ClusterEventType.HINT_ENQUEUED, ClusterEventType.HINT_DELIVERED],
};

// Badge background colour keyed by event type
export const BADGE_COLOR: Partial<Record<ClusterEventType, string>> = {
    [ClusterEventType.NODE_DOWN]: 'var(--color-danger)',
    [ClusterEventType.REPLICA_FAILED]: 'var(--color-danger)',
    [ClusterEventType.QUORUM_FAILED]: 'var(--color-danger)',
    [ClusterEventType.NODE_SUSPECT]: 'var(--color-warning)',
    [ClusterEventType.NODE_UP]: 'var(--color-success)',
    [ClusterEventType.QUORUM_SUCCESS]: 'var(--color-success)',
    [ClusterEventType.SYNC_COMPLETE]: 'var(--color-success)',
    [ClusterEventType.SYNC_STARTED]: 'var(--color-accent)',
    [ClusterEventType.REPLICA_WRITE]: 'var(--color-accent)',
    [ClusterEventType.CACHE_GET]: '#ff9800', // hot-key orange
    [ClusterEventType.HINT_ENQUEUED]: '#9c6fff',
    [ClusterEventType.HINT_DELIVERED]: '#9c6fff',
};

export const MERGEABLE_TYPES = new Set<ClusterEventType>([
    ClusterEventType.NODE_UP,
    ClusterEventType.NODE_SUSPECT,
    ClusterEventType.NODE_DOWN,
])