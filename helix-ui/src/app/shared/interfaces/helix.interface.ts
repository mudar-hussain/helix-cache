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
    predicted: number;
}

export interface CacheStats {
    size: number;
    hitCount: number;
    missCount: number;
    hitRatio: number;
}

export interface Node {
    id: string;
    host: string;
    port: number;
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
    status: 'ok' | 'error';
    statusCode: number;
    latencyMs: number;
    body: string;
    primaryNode?: string;
}

export interface NodeConfig {
    id: string;
    baseUrl: string;
    color: string;
}