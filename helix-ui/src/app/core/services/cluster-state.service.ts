import { computed, inject, Injectable, OnDestroy, signal } from "@angular/core";
import { ClusterApiService } from "./cluster-api.service";
import { SseService } from "./sse.service";
import { catchError, EMPTY, interval, startWith, Subscription, switchMap } from "rxjs";
import { ClusterEvent, CacheStats, Node, NodeStatusResponse, RingNodeResponse, ReplicaNodes, ClusterEventEntry } from "../../shared/interfaces/helix.interface";
import { ClusterEventType, NodeStatus } from "../enums/helix.enum";
import { environment } from "../../../environments/environment";
import { MERGEABLE_TYPES } from "../constants/app.constant";

@Injectable({
    providedIn: 'root'
})
export class ClusterStateService implements OnDestroy {
    private readonly sse = inject(SseService);
    private subs = new Subscription();

    //Reactive state for the cluster
    readonly nodes = signal<NodeStatusResponse[]>([]);
    readonly ring = signal<RingNodeResponse[]>([]);
    readonly stats = signal<CacheStats | null>(null);

    //Derived state for the cluster
    readonly aliveCount = computed(() => this.nodes().filter(node => node.nodeStatus === NodeStatus.UP).length);
    readonly totalKeys = computed(() => this.stats()?.size ?? 0);
    readonly hitRatio = computed(() => this.stats()?.hitRatio ?? 0);

    readonly events$ = this.sse.events$;
    readonly replicaNodes = signal<ReplicaNodes | null>(null);
    private highlightTimer: ReturnType<typeof setTimeout> | null = null;

    private readonly MAX_EVENTS = 500;
    readonly eventLog = signal<ClusterEventEntry[]>([]);

    constructor(private clusterApi: ClusterApiService) {
        this.startPolling();
        this.subscribeToNodeEvents();
    }

    ngOnDestroy() {
        this.subs.unsubscribe();
    }

    setReplicaNodes(replica: ReplicaNodes): void {
        if (this.highlightTimer) {
            clearTimeout(this.highlightTimer);
        }
        this.replicaNodes.set(replica);
        this.highlightTimer = setTimeout(() => this.replicaNodes.set(null), 2000);
    }

    clearReplicaNodes(): void {
        if (this.highlightTimer) {
            clearTimeout(this.highlightTimer);
        }
        this.replicaNodes.set(null);
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
                console.log(idx);
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
}