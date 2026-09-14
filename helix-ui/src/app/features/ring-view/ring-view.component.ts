import { Component, inject, ElementRef, ViewChild, AfterViewInit, OnDestroy, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClusterStateService } from '../../core/services/cluster-state.service';
import { NodeColorPipe } from '../../shared/components/node-color.pipe';
import { Node, NodeStatusResponse, RingNodeResponse } from '../../shared/interfaces/helix.interface';
import { environment } from '../../../environments/environment';
import { FormsModule } from '@angular/forms';
import { ClusterApiService } from '../../core/services/cluster-api.service';
import { single } from 'rxjs';
@Component({
    selector: 'app-ring-view',
    standalone: true,
    imports: [CommonModule, FormsModule, NodeColorPipe],
    templateUrl: './ring-view.component.html',
    styleUrl: './ring-view.component.css',
})
export class RingViewComponent implements AfterViewInit, OnDestroy {

    @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
    protected readonly state = inject(ClusterStateService);
    private readonly clusterApi = inject(ClusterApiService)
    private rafId = 0;
    private readonly colors: Record<string, string> = Object.fromEntries(environment.nodes.map(n => [n.id, n.color]));

    //key route lookup state
    searchKey = '';
    routeNodes = signal<Node[]>([]);
    routeError = signal<string | null>(null);
    isSearching = signal(false);
    private readonly fx = effect(() => {
        const nodes = this.state.nodes();
        const ring = this.state.ring();
        if (this.canvasRef) this.schedule(nodes, ring);
    });

    ngAfterViewInit() {
        this.schedule(this.state.nodes(), this.state.ring());
    }

    ngOnDestroy() {
        cancelAnimationFrame(this.rafId);
        this.fx.destroy();
    }

    //Key search
    onSearch(): void {
        const key = this.searchKey.trim();
        if (!key) {
            this.routeNodes.set([]);
            this.routeError.set(null);
            this.schedule(this.state.nodes(), this.state.ring());
            return;
        }
        this.isSearching.set(true);
        this.routeError.set(null);
        this.clusterApi.getRouteForKey(key).subscribe({
            next: (nodes) => {
                this.routeNodes.set(nodes);
                this.isSearching.set(false);
                this.schedule(this.state.nodes(), this.state.ring());
            },
            error: (err) => {
                this.routeError.set(err.status === 404 ? 'Key not found' : 'Lookup failed');
                this.routeNodes.set([]);
                this.isSearching.set(false);
            }
        });
    }

    clearSearch(): void {
        this.searchKey = '';
        this.routeNodes.set([]);
        this.routeError.set(null);
        this.schedule(this.state.nodes(), this.state.ring());
    }


    //Canvas UI
    private schedule(nodes: NodeStatusResponse[], ring: RingNodeResponse[]) {
        cancelAnimationFrame(this.rafId);
        this.rafId = requestAnimationFrame(() => this.draw(nodes, ring));
    }

    private draw(nodes: NodeStatusResponse[], ring: RingNodeResponse[]): void {
        const canvas = this.canvasRef?.nativeElement;
        if (!canvas) return;

        const ctx: CanvasRenderingContext2D | null = canvas.getContext('2d') as CanvasRenderingContext2D;
        if (!ctx) return;

        const W = canvas.clientWidth, H = canvas.clientHeight;
        canvas.width = W;
        canvas.height = H;

        const cx = W / 2, cy = H / 2;
        const ringR = Math.min(W, H) * 0.30;
        const nodeR = Math.min(W, H) * 0.42;

        ctx.clearRect(0, 0, W, H);

        //Highlighted node ids from route lookup
        const highlighted = new Set(this.routeNodes().map(n => n.id));
        const isPrimary = this.routeNodes().length > 0 ? this.routeNodes()[0].id : null;


        // Node positions (pentagon layout)
        const ids = nodes.map(n => n.nodeId);
        const N = ids.length || 5;
        const pos = new Map<string, { x: number; y: number }>(ids.map((id, i) => {
            const a = (2 * Math.PI * i / N) - Math.PI / 2;
            return [id, { x: cx + nodeR * Math.cos(a), y: cy + nodeR * Math.sin(a) }];
        }));

        // Hash ring circle
        ctx.beginPath();
        ctx.arc(cx, cy, ringR, 0, Math.PI * 2);
        ctx.strokeStyle = 'rgba(0,200,255,0.18)';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        // Pentagon edges
        ctx.beginPath();
        ids.forEach((id, i) => {
            const p = pos.get(id)!;
            i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y);
        });
        ctx.closePath();
        ctx.strokeStyle = 'rgba(0,200,255,0.10)';
        ctx.lineWidth = 1;
        ctx.stroke();

        // Spokes center node
        pos.forEach((p, id) => {
            const color = this.colors[id] ?? '#607d8b'
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.lineTo(p.x, p.y);
            ctx.strokeStyle = highlighted.has(id) ? color + '55' : color + '22';
            ctx.lineWidth = highlighted.has(id) ? 1.5 : 1;
            ctx.stroke();
        });

        // Virtual nodes on ring
        const vnByNode = new Map<string, RingNodeResponse[]>();
        ring.forEach(r => {
            if (!vnByNode.has(r.nodeId)) vnByNode.set(r.nodeId, []);
            vnByNode.get(r.nodeId)!.push(r);
        });

        vnByNode.forEach((vns, id) => {
            const color = this.colors[id] ?? '#607d8b';
            vns.forEach(vn => {
                const a = (vn.hash / 2_147_483_647) * Math.PI * 2 - Math.PI / 2;
                const vx = cx + ringR * Math.cos(a);
                const vy = cy + ringR * Math.sin(a);
                this.miniPentagon(ctx, vx, vy, highlighted.has(id) ? 7 : 5, a, color, highlighted.has(id));
            });
        });

        // key position marker on ring (if search active)
        if (this.searchKey.trim() && this.routeNodes().length > 0) {
            this.drawKeyMarker(ctx, cx, cy, ringR, this.searchKey.trim());
        }

        // Physical nodes
        nodes.forEach(node => {
            const p = pos.get(node.nodeId);
            if (!p) return;

            const color = this.colors[node.nodeId] ?? '#607d8b';
            const isDown = node.nodeStatus === 'DOWN';
            const isHighlight = highlighted.has(node.nodeId);
            const isPrim = node.nodeId === isPrimary;

            // Glow
            const glowR = isPrim ? 38 : isHighlight ? 32 : 28;
            const grad = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, 28);
            grad.addColorStop(0, color + (isPrim ? '70' : isHighlight ? '55' : '40'));
            grad.addColorStop(1, 'rgba(0,0,0,0)');
            ctx.beginPath();
            ctx.arc(p.x, p.y, 28, 0, Math.PI * 2);
            ctx.fillStyle = grad;
            ctx.fill();

            //Outer ring for Primary Node
            if (isPrim) {
                ctx.beginPath();
                ctx.arc(p.x, p.y, 24, 0, Math.PI * 2);
                ctx.strokeStyle = color + 'cc';
                ctx.lineWidth = 1.5;
                ctx.stroke();
            }

            // Pentagon body - larger when highlighted
            const pentR = isHighlight ? 22 : 18;
            this.pentagon(ctx, p.x, p.y, pentR, -Math.PI / 2, color, isDown, isHighlight);

            //Replica index label inside pentagon for highlighted nodes
            if (isHighlight) {
                const idx = this.routeNodes().findIndex(n => n.id === node.nodeId);
                ctx.font = '900 9px Courier New';
                ctx.fillStyle = color;
                ctx.textAlign = 'center';
                ctx.textBaseline = 'middle';
                ctx.fillText(idx === 0 ? 'P' : `R${idx}`, p.x, p.y);
            }

            // Node Id Labels
            ctx.font = '700 9px Courier New';
            ctx.fillStyle = isHighlight ? color : color + 'aa';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(node.nodeId, p.x, p.y + (isHighlight ? 30 : 28));

            //key count
            ctx.font = '500 8px Courier New';
            ctx.fillStyle = 'rgba(200,232,255,0.5)';
            ctx.fillText(node.keyCount + ' Keys', p.x, p.y + (isHighlight ? 40 : 38));
        });

        // Center dot
        ctx.beginPath();
        ctx.arc(cx, cy, 4, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(0,200,255,0.6)';
        ctx.fill();
    }

    // Draws a pulsing marker on the ring at the key's hash position

    private drawKeyMarker(ctx: CanvasRenderingContext2D, cx: number, cy: number, ringR: number, key: string): void {

        // Simple hash for visual position same logic as backend SHA-256 approximation
        let hash = 5381;
        for (let i = 0; i < key.length; i++) {
            hash = ((hash << 5) + hash) + key.charCodeAt(i);
            hash = hash & 0x7fffffff; // keep positive 31-bit
        }
        const angle = (hash / 2_147_483_647) * Math.PI * 2 - Math.PI / 2;
        const mx = cx + ringR * Math.cos(angle);
        const my = cy + ringR * Math.sin(angle);

        // Outer pulse ring
        ctx.beginPath();
        ctx.arc(mx, my, 9, 0, Math.PI * 2);
        ctx.strokeStyle = '#ffffff55';
        ctx.lineWidth = 1;
        ctx.stroke();

        // Inner dot
        ctx.beginPath();
        ctx.arc(mx, my, 5, 0, Math.PI * 2);
        ctx.fillStyle = '#ffffff';
        ctx.fill();

        // Key label
        ctx.font= '700 8px Courier New';
        ctx.fillStyle = '#ffffff';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'bottom';
        const label = key.length > 12 ? key.slice(0, 11) + '...' : key;
        ctx.fillText(label, mx, my - 10);
    }

    private pentagon(ctx: CanvasRenderingContext2D, x: number, y: number, r: number, a0: number, color: string, dim: boolean, bright: boolean = false): void {
        ctx.beginPath();
        for (let i = 0; i < 5; i++) {
            const a = a0 + i * 2 * Math.PI / 5;
            const px = x + r * Math.cos(a);
            const py = y + r * Math.sin(a);
            if(i == 0) {
                ctx.moveTo(px, py);
            } else {
                ctx.lineTo(px, py);
            }
        }
        ctx.closePath();
        ctx.fillStyle = color + (dim ? '18' : bright ? '40' : '28');
        ctx.strokeStyle = color + (dim ? '55' : bright ? 'ff' : 'cc');
        ctx.lineWidth = bright ? 2 : 1.5;
        ctx.fill();
        ctx.stroke();
    }

    private miniPentagon(ctx: CanvasRenderingContext2D, x: number, y: number, r: number, a0: number, color: string, bright: boolean = false): void {
        ctx.beginPath();
        for (let i = 0; i < 5; i++) {
            const a = a0 + i * 2 * Math.PI / 5;
            const px = x + r * Math.cos(a);
            const py = y + r * Math.sin(a);
            if(i == 0) {
                ctx.moveTo(px, py);
            } else {
                ctx.lineTo(px, py);
            }
        }
        ctx.closePath();
        ctx.fillStyle = color + bright ? '88' : '55';
        ctx.strokeStyle = color + bright ? 'ff' : 'cc';
        ctx.lineWidth = bright ? 1.5 : 1;
        ctx.fill();
        ctx.stroke();
    }
}
