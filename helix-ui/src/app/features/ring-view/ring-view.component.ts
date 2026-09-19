import { CommonModule } from "@angular/common";
import { NodeColorPipe } from "../../shared/components/node-color.pipe";
import { Component, computed, inject, OnInit, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { ClusterStateService } from "../../core/services/cluster-state.service";
import { interval, startWith, Subscription, switchMap } from "rxjs";
import { NodeDistributionResponse } from "../../shared/interfaces/helix.interface";
import { ClusterApiService } from "../../core/services/cluster-api.service";
import { StatCardComponent } from "../../shared/components/stat-card/stat-card.component";
import { NodeStatus } from "../../core/enums/helix.enum";


interface NodePos { id: string; x: number; y: number; }

@Component({
    selector: 'app-ring-view',
    standalone: true,
    imports: [CommonModule, NodeColorPipe, StatCardComponent],
    templateUrl: './ring-view.component.html',
    styleUrl: './ring-view.component.css',
})
export class RingViewComponent implements OnInit {

    protected readonly clusterState = inject(ClusterStateService);
    protected readonly clusterApi = inject(ClusterApiService);
    protected readonly N = environment.nodes.length;
    private readonly colors: Record<string, string> = Object.fromEntries(
        environment.nodes.map(n => [n.id, n.color])
    );
    readonly width = 800
    readonly height = 600;
    readonly cx = this.width / 2;
    readonly cy = this.height / 2;
    readonly ringR = Math.min(this.width, this.height) * 0.30;
    readonly nodeR = Math.min(this.width, this.height) * 0.42;

    private sub = new Subscription();
    readonly distribution = signal<NodeDistributionResponse[]>([]);
    readonly totalKeyCount = computed<number>(() => {
        const nodeDistributions = this.distribution();
        let totalKeyCount = 0;
        for(let node of nodeDistributions) {
            totalKeyCount += node.keyCount;
        }
        return totalKeyCount;
    });

    readonly nodePositions = computed<NodePos[]>(() => {
        const nodes = this.clusterState.nodes();
        const nodesLength = nodes.length || 5;
        return nodes.map((n, i) => {
            const a = (2 * Math.PI * i / nodesLength) - Math.PI / 2;
            return { id: n.nodeId, x: this.cx + this.nodeR * Math.cos(a), y: this.cy + this.nodeR * Math.sin(a) };
        });
    });

    readonly edgePoints = computed<string>(() =>
        this.nodePositions().map(p => `${p.x}, ${p.y}`).join(' ')
    );

    readonly vnodePositions = computed(() =>
        this.clusterState.ring().map(vn => {
            const a = vn.normalizedPosition * Math.PI * 2 - Math.PI / 2;
            return { ...vn, x: this.cx + this.ringR * Math.cos(a), y: this.cy + this.ringR * Math.sin(a) };
        })
    );

    readonly highlighted = computed(() => {
        const highlightedNodes = new Set<string>();
        const replicaNodes = this.clusterState.replicaNodes(); 
        if(replicaNodes === null) return highlightedNodes;
        highlightedNodes.add(replicaNodes?.primaryNode.id)
        for(const node of replicaNodes.replicaNodes) {
            highlightedNodes.add(node.id);
        }
        return highlightedNodes;
    });

    readonly primaryId = computed(() =>
        this.clusterState.replicaNodes()?.primaryNode.id ?? null
    );

    // Replica travel dots primary each active replica

    readonly travelDots = computed(() => {
        const replicaNodes = this.clusterState.replicaNodes();
        if (replicaNodes === null) return [];
        const primaryNodeId = replicaNodes.primaryNode.id;
        const primaryPos = this.getPos(primaryNodeId);
        const animKey = Date.now();
        return replicaNodes.replicaNodes
            .filter(r => this.nodeStatus(r.id) === 'UP')
            .map((r, i) => {
                const replicaPos = this.getPos(r.id);
                const dx = replicaPos.x - primaryPos.x;
                const dy = replicaPos.y - primaryPos.y;
                return {
                    key: `${r.id}-${animKey}`,
                    id: r.id,
                    startX: primaryPos.x,
                    startY: primaryPos.y,
                    dx,
                    dy,
                    color: this.color(primaryNodeId),
                    delay: i * 0.2
                };

            });
    });

    readonly burstDots = computed(() => {
        const burst = this.clusterState.ringBurst();
        if(!burst) return []; 

        const liveNodes = this.clusterState.nodes()
            .filter(n => n.nodeStatus === 'UP')
            .map(n => this.getPos(n.nodeId));

        const dots: Array<{
            key: string; startX: number; startY: number;
            dx: number; dy: number; color: string; delay: number;
        }> = [];

        let idx = 0;
        liveNodes.forEach(from => {
            liveNodes.forEach(to => {
                if(from.id === to.id) return;
                dots.push({
                    key: `burst-${from.id}-${to.id}-${burst}`,
                    startX: from.x,
                    startY: from.y,
                    dx: to.x - from.x,
                    dy: to.y - from.y,
                    color: this.color(from.id),
                    delay: idx * 0.05,
                });
                idx++;
            });
        });
        return dots;
    })

    ngOnInit(): void {
        this.sub.add(
            interval(environment.pollIntervals.distribution)
                .pipe(
                    startWith(0),
                    switchMap(() => this.clusterApi.getDistribution())
                )
                .subscribe(data => this.distribution.set(data))
        );
    }

    getDistributionPercentage(nodeId: string): string {
        const node = this.distribution().find(
            item => item.nodeId === nodeId
        );

        return node ? `${node.percentage.toFixed(2)}%` : '0%';
    }

    color(nodeId: string): string { return this.colors[nodeId] ?? '#607d8b'; }
    isHighlit(nodeId: string): boolean { return this.highlighted().has(nodeId); }
    isPrimary(nodeId: string): boolean { return this.primaryId() === nodeId; }

    replicaLabel(nodeId: string): string {
        const replicaNodes = this.clusterState.replicaNodes(); 
        if(replicaNodes == null) return '';      
        if(nodeId === replicaNodes.primaryNode.id) {
            return 'P';
        } else {
            const idx = replicaNodes?.replicaNodes.findIndex(n => n.id === nodeId); 
            return `R${idx+1}`;
        }
    }

    getPos(nodeId: string): NodePos {
        return this.nodePositions().find(p => p.id === nodeId) ?? { id: nodeId, x: this.cx, y: this.cy };
    }

    nodeStatus(nodeId: string): string {
        return this.clusterState.nodes().find(n => n.nodeId === nodeId)?.nodeStatus ?? '';
    }

    isNodeAlive(nodeId: string): boolean {
        return this.nodeStatus(nodeId) !== NodeStatus.DOWN;
    }

    pentagonPoints(cx: number, cy: number, r: number): string {
        return Array.from({ length: 5 }, (_, i) => {
            const a = (2 * Math.PI * i / 5) - Math.PI / 2;
            return `${cx + r * Math.cos(a)},${cy + r * Math.sin(a)}`;
        }).join(' ');
    }
}