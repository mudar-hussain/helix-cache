import { Component, inject, ElementRef, ViewChild, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule} from '@angular/common';
import { ClusterStateService } from '../../core/services/cluster-state.service';
import { NodeColorPipe } from '../../shared/components/node-color.pipe';
import { NodeStatusResponse, RingNodeResponse} from '../../shared/interfaces/helix.interface';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-ring-view',
    standalone: true,
    imports: [CommonModule, NodeColorPipe],
    templateUrl: './ring-view.component.html',
    styleUrl: './ring-view.component.css',
})
export class RingViewComponent implements AfterViewInit, OnDestroy {

    @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
    protected readonly state = inject(ClusterStateService);
    private rafId = 0;
    private readonly colors = Object.fromEntries(environment.nodes.map (n => [n.id, n.color]));
    
    private readonly fx = effect (() => {
        const nodes = this.state.nodes();
        const ring = this.state.ring();
        if (this.canvasRef) this.schedule(nodes, ring);
    });

    ngAfterViewInit() { 
        this.schedule(this.state.nodes(), this.state.ring()); 
    }

    ngOnDestroy() { 
        cancelAnimationFrame(this.rafId); this.fx.destroy(); 
    }

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
        canvas.width = W; canvas.height = H;

        const cx = W/2, cy = H/2;
        const ringR = Math.min(W, H) * 0.30;
        const nodeR = Math.min(W, H) * 0.42;
        ctx.clearRect(0, 0, W, H);

        // Node positions (pentagon layout)
        const ids = nodes.map (n => n.nodeId);
        const N = ids.length || 5;

        const pos = new Map(ids.map((id, i) => {
            const a = (2 * Math.PI * i/N) - Math.PI/2;
            return [id, {x: cx + nodeR * Math.cos(a), y: cy + nodeR * Math.sin(a) }];
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
            ctx.beginPath(); 
            ctx.moveTo(cx, cy); 
            ctx.lineTo(p.x, p.y);

            ctx.strokeStyle = (this.colors[id] ?? '#607d8b') + '22';
            ctx.lineWidth = 1; 
            ctx.stroke();
        });

        // Virtual nodes on ring
        const vnByNode = new Map<string, RingNodeResponse []>();

        ring.forEach(r => { 
            if (!vnByNode.has(r.nodeId)) vnByNode.set(r.nodeId, []); 
            vnByNode.get(r.nodeId)!.push(r); 
        });

        vnByNode.forEach((vns, id) => {
            const color = this.colors[id] ?? '#607d8b';
            vns.forEach (vn => {
                const a = (vn.hash/2147483647) * Math.PI * 2 - Math.PI / 2;
                this.miniPentagon(ctx, cx + ringR * Math.cos(a), cy + ringR * Math.sin(a), 5, a, color);
            });
        });

        // Physical nodes

        nodes.forEach (node => {
            const p = pos.get(node.nodeId); if (!p) return;
            const color = this.colors[node.nodeId] ?? '#607d8b';
            const down = node.nodeStatus === 'DOWN';

            // Glow
            const g = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, 28);
            g.addColorStop(0, color + '40'); 
            g.addColorStop (1, 'transparent');
            ctx.beginPath(); 
            ctx.arc(p.x, p.y, 28, 0, Math.PI * 2); 
            ctx.fillStyle = g; 
            ctx.fill();

            // Pentagon
            this.pentagon(ctx, p.x, p.y, 18, -Math.PI/2, color, down);

            // Labels
            ctx.font = '700 9px Courier New'; 
            ctx.fillStyle = color; 
            ctx.textAlign = 'center'; 
            ctx.textBaseline = 'middle';

            ctx.fillText (node.nodeId, p.x, p.y + 28);
            ctx.font = '500 8px Courier New'; 
            ctx.fillStyle = 'rgba(200,232,255,0.5)';
            ctx.fillText(`${node.keyCount}k`, p.x, p.y + 38);
        });

        // Center dot
        ctx.beginPath(); 
        ctx.arc(cx, cy, 4, 0, Math.PI * 2);

        ctx.fillStyle = 'rgba(0,200,255,0.6)'; 
        ctx.fill();
    }

    private pentagon(ctx: CanvasRenderingContext2D, x: number, y: number, r: number, a0: number, color: string, dim: boolean): void {
        ctx.beginPath();
        for (let i=0; i < 5; i++) { 
            const a = a0 + i * 2 * Math.PI / 5;
            i == 0 ? ctx.moveTo(x + r * Math.cos(a), y + r * Math.sin(a)) : ctx.lineTo(x+r * Math.cos(a), y + r * Math.sin(a)); 
        }
        ctx.closePath();
        ctx.fillStyle = color + (dim? '18': '28'); 
        ctx.strokeStyle = color + (dim? '55': 'cc'); 
        ctx.lineWidth = 1.5; 
        ctx.fill(); 
        ctx.stroke();
    }

    private miniPentagon(ctx: CanvasRenderingContext2D, x: number, y: number, r: number, a0: number, color: string): void {
        ctx.beginPath();
        for (let i=0; i < 5; i++) { 
            const a = a0 + i * 2 * Math.PI / 5;
            i == 0 ? ctx.moveTo(x + r * Math.cos(a), y + r * Math.sin(a)) : ctx.lineTo(x+r * Math.cos(a), y + r * Math.sin(a)); 
        }
        ctx.closePath();
        ctx.fillStyle = color + '55'; 
        ctx.strokeStyle = color + 'cc'; 
        ctx.lineWidth = 1; 
        ctx.fill(); 
        ctx.stroke();
    }

}

