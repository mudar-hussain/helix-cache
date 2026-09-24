import { computed, inject, Injectable, OnDestroy, signal } from "@angular/core";
import { ClusterApiService } from "./cluster-api.service";
import { SseService } from "./sse.service";
import { catchError, EMPTY, forkJoin, interval, startWith, Subscription, switchMap } from "rxjs";
import { ClusterEvent, ClusterStats, NodeStatusResponse, RingNodeResponse, ReplicaNodes, ClusterEventEntry, PartitionConfig } from "../../shared/interfaces/helix.interface";
import { ClusterEventType, NodeStatus } from "../enums/helix.enum";
import { environment } from "../../../environments/environment";
import { MERGEABLE_TYPES } from "../constants/app.constant";
import { AdminApiService } from "./admin-api.service";

@Injectable({
    providedIn: 'root'
})
export class ClusterStateService implements OnDestroy {
    private readonly sse = inject(SseService);
    private subs = new Subscription();

    //Reactive state for the cluster
    readonly nodes = signal<NodeStatusResponse[]>([]);
    readonly ring = signal<RingNodeResponse[]>([]);
    readonly stats = signal<ClusterStats | null>(null);
    readonly ringBurst = signal<number>(0);
    readonly partition = signal<PartitionConfig | null>(null);

    //Derived state for the cluster
    readonly aliveCount = computed(() => this.nodes().filter(node => node.nodeStatus !== NodeStatus.DOWN).length);
    readonly totalKeys = computed(() => this.stats()?.totalKeys ?? 0);
    readonly replicationFactor = computed(() => this.stats()?.replicationFactor ?? 0);
    readonly virtualNodesPerNode = computed(() => this.stats()?.virtualNodesPerNode ?? 0);
    readonly writeQuorum = computed(() => this.stats()?.writeQuorum ?? 0);
    readonly readQuorum = computed(() => this.stats()?.readQuorum ?? 0);
    readonly virtualNodes = computed(() => this.aliveCount() * this.virtualNodesPerNode());

    readonly events$ = this.sse.events$;
    readonly replicaNodes = signal<ReplicaNodes | null>(null);
    private highlightTimer: ReturnType<typeof setTimeout> | null = null;
    private burstTimer: ReturnType<typeof setTimeout> | null = null;

    private readonly MAX_EVENTS = 500;
    readonly eventLog = signal<ClusterEventEntry[]>([]);

    constructor(private clusterApi: ClusterApiService, private adminApi: AdminApiService) {
        this.startPolling();
        this.subscribeToNodeEvents();
        this.syncPartition();
    }

    ngOnDestroy() {
        this.subs.unsubscribe();
    }

    setReplicaNodes(replica: ReplicaNodes): void {
        if (this.highlightTimer) {
            clearTimeout(this.highlightTimer);
        }
        this.replicaNodes.set(replica);
        this.highlightTimer = setTimeout(() => this.replicaNodes.set(null), 2500);
    }

    clearReplicaNodes(): void {
        if (this.highlightTimer) {
            clearTimeout(this.highlightTimer);
        }
        this.replicaNodes.set(null);
    }

    triggerRingBurst(): void {
        if (this.burstTimer) clearTimeout(this.burstTimer);
        this.ringBurst.set(Date.now());
        this.burstTimer = setTimeout(() => this.ringBurst.set(0), 2500);
    }

    private startPolling() {
        const { nodes, distribution } = environment.pollIntervals;
        this.subs.add(
            interval(nodes).pipe(startWith(0), switchMap(() => this.clusterApi.getNodes()
                .pipe(catchError(() => EMPTY)))
            ).subscribe(nodes => this.nodes.set(nodes))
        );
        this.subs.add(
            interval(nodes).pipe(startWith(0), switchMap(() => this.clusterApi.getRing()
                .pipe(catchError(() => EMPTY)))
            ).subscribe(ring => this.ring.set(ring))
        );
        this.subs.add(
            interval(distribution).pipe(startWith(0), switchMap(() => this.clusterApi.getStats()
                .pipe(catchError(() => EMPTY)))
            ).subscribe(stats => this.stats.set(stats))
        );
    }

    private subscribeToNodeEvents() {
        //on any node status change, refresh the nodes and ring state
        this.subs.add(
            this.events$.subscribe(event => {
                this.appendEvent(event);
                if ([ClusterEventType.NODE_UP, ClusterEventType.NODE_DOWN, ClusterEventType.NODE_SUSPECT].includes(event.clusterEventType)) {
                    this.subs.add(this.clusterApi.getNodes().subscribe(nodes => this.nodes.set(nodes)));
                    this.subs.add(this.clusterApi.getRing().subscribe(ring => this.ring.set(ring)));
                }
                if ([ClusterEventType.NODE_DOWN].includes(event.clusterEventType)) {
                    this.triggerRingBurst();
                }
            })
        );
    }

    private appendEvent(event: ClusterEvent): void {
        this.eventLog.update(prev => {
            if (MERGEABLE_TYPES.has(event.clusterEventType)
        ) {

                // Find the most-recent entry that matches this event's merge key
                const mergeKey = (e: ClusterEventEntry) =>
                    e.event.clusterEventType === event.clusterEventType &&
                    e.event.nodeId === event.nodeId &&
                    e.event.detail === event.detail;

                const idx = prev.findIndex(mergeKey);
                if (idx !== -1 && idx<5) {

                    // Merge: bump count + update timestamp on existing entry
                    const updated: ClusterEventEntry[] = [...prev];
                    updated[idx] = {
                        ...updated[idx],
                        count: updated[idx].count + 1,
                        lastTimestamp: event.eventTimestamp,
                    };
                    return updated;
                }
            }
            // Normal prepend
            const entry: ClusterEventEntry = {
                event,
                count: 1,
                lastTimestamp: event.eventTimestamp
            };
            return [entry, ...prev].slice(0, this.MAX_EVENTS);
        });
    }

    setPartition(config: PartitionConfig): void {
        this.partition.set(config);
    }

    healPartition(): void {
        this.partition.set(null);
    }

    private syncPartition() {
        const nodeEntries = environment.nodes;

        const calls = nodeEntries.map(n => 
            this.adminApi.getPartition(n.baseUrl).pipe(catchError(() => EMPTY))
        );

        forkJoin(calls) .subscribe((results: {nodeId: string; blockedPeers: string[]}[]) => {
            const blocking = results.filter(r => r.blockedPeers.length > 0);
            if(blocking.length === 0) return; //no partition exists in backend

            const allNodeIds: string[] = nodeEntries.map(n => n.id);
            const blockMap = new Map<string, Set<string>>();
            results.forEach(r => blockMap.set(r.nodeId, new Set(r.blockedPeers)));

            const neutral = allNodeIds.filter(id => (blockMap.get(id)?.size ?? 0) === 0);

            const firstBlocker = results.find(r => r.blockedPeers.length > 0);
            if(!firstBlocker) return;

            const explicitB = firstBlocker.blockedPeers.filter(id => !neutral.includes(id));
            const explicitA = allNodeIds.filter(id => !explicitB.includes(id) && !neutral.includes(id));

            if(explicitA.length === 0 || explicitB.length === 0) return;

            this.partition.set({
                groupA: [...explicitA, ...neutral],
                groupB: [...explicitB, ...neutral],
                explicitA,
                explicitB,
                neutral
            });
        });
    }
}
