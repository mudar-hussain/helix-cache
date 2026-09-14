import { CommonModule } from "@angular/common";
import { NodeColorPipe } from "../../shared/components/node-color.pipe";
import { Component, computed, inject } from "@angular/core";
import { environment } from "../../../environments/environment";
import { ClusterStateService } from "../../core/services/cluster-state.service";


interface NodePos { id: string; x: number; y: number; }

@Component({
    selector: 'app-ring-view',
    standalone: true,
    imports: [CommonModule, NodeColorPipe],
    templateUrl: './ring-view.component.html',
    styleUrl: './ring-view.component.css',
})
export class RingViewComponent {

    protected readonly state = inject(ClusterStateService);
    private readonly colors: Record<string, string> = Object.fromEntries(
        environment.nodes.map(n => [n.id, n.color])
    );
    readonly width = 800
    readonly height = 600;
    readonly cx = this.width / 2;
    readonly cy = this.height / 2;
    readonly ringR = Math.min(this.width, this.height) * 0.30;
    readonly nodeR = Math.min(this.width, this.height) * 0.42;

    readonly nodePositions = computed<NodePos[]>(() => {
        const nodes = this.state.nodes();
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
        this.state.ring().map(vn => {
            const a = (vn.hash / 2_147_483_647) * Math.PI * 2 - Math.PI / 2;
            return { ...vn, x: this.cx + this.ringR * Math.cos(a), y: this.cy + this.ringR * Math.sin(a) };
        })
    );

    readonly highlighted = computed(() =>
        new Set<string>(this.state.routeNodes().map(n => n.id))
    );

    readonly primaryId = computed(() =>
        this.state.routeNodes().length > 0 ? this.state.routeNodes()[0].id : null
    );

    // Replica travel dots primary each active replica

    readonly travelDots = computed(() => {
        const nodes = this.state.routeNodes();
        if (nodes.length < 2) return [];
        const primary = nodes[0];
        const primaryPos = this.getPos(primary.id);
        return nodes.slice(1)
            .filter(r => this.nodeStatus(r.id) == 'UP')
            .map((r, i) => {

                const replicaPos = this.getPos(r.id);

                return {

                    id: r.id,

                    x1: primaryPos.x,

                    y1: primaryPos.y,

                    x2: replicaPos.x,

                    y2: replicaPos.y,

                    color: this.color(primary.id),

                    delay: i * 0.15

                };

            });

    });

    color(nodeId: string): string { return this.colors[nodeId] ?? '#607d8b'; }
    isHighlit(nodeId: string): boolean { return this.highlighted().has(nodeId); }
    isPrimary(nodeId: string): boolean { return this.primaryId() === nodeId; }

    replicaLabel(nodeId: string): string {
        const idx = this.state.routeNodes().findIndex(n => n.id === nodeId);
        return idx === 0 ? 'P' : `R${idx}`;
    }

    getPos(nodeId: string): NodePos {


        return this.nodePositions().find(p => p.id === nodeId) ?? { id: nodeId, x: this.cx, y: this.cy };
    }

    nodeStatus(nodeId: string): string {

        return this.state.nodes().find(n => n.nodeId === nodeId)?.nodeStatus ?? '';
    }

    pentagonPoints(cx: number, cy: number, r: number): string {
        return Array.from({ length: 5 }, (_, i) => {
            const a = (2 * Math.PI * i / 5) - Math.PI / 2;
            return `${cx + r * Math.cos(a)},${cy + r * Math.sin(a)}`;
        }).join(' ');
    }
}