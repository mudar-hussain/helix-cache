import { ClusterEventType, NodeStatus, Severity } from "../../core/enums/helix.enum";

export interface NodeStatusResponse {
    nodeId: string;
    address: string;
    nodeStatus: NodeStatus;
    isLocal: boolean;
    keyCount: number;
    missedHeartbeats: number;
    lastSeenAt: string;
}

export interface RingNodeResponse {
    hash: number;
    nodeId: string;
    address: string;
    replicaIndex: number;
    normalizedPosition: number;
}

export interface NodeDistributionResponse {
    nodeId: string;
    keyCount: number;
    percentage: number;
}

export interface HotKeyPredictionResponse {
    key: string;
    emaScore: number;
    recentRate: number;
    totalAccess: number;
    predicted: boolean;
}

export interface ClusterStats {
    totalKeys: number;
    replicationFactor: number;
    writeQuorum: number;
    readQuorum: number;
    virtualNodesPerNode: number;
}

export interface Node {
    id: string;
    host: string;
    port: number;
}

export interface ReplicaNodes {
    key: string;
    primaryNode: Node;
    replicaNodes: Node[];
}

export interface CacheResponse {
    key: string;
    value: string;
    version: number;
    primaryNode: Node;
    createdAt: string;
    ttlSeconds: string | null;
    lastAccessedAt: string;
}

export interface ClusterEvent {
    clusterEventType: ClusterEventType;
    nodeId: string;
    key: string | null;
    detail: string;
    severity: Severity;
    eventTimestamp: string;
}

export interface OpResult {
    latencyMs: number;
    body: CacheResponse;
}

export interface NodeConfig {
    id: string;
    baseUrl: string;
    color: string;
}

export interface ClusterEventEntry {
    event: ClusterEvent;
    count: number;      // 1 = single event, >1 = merged duplicates
    lastTimestamp: string;      // timestamp of the most-recent merged event
}

export interface SeedRequestResult {
    total: number;
    succeeded: number;      
    failed: number;      
}