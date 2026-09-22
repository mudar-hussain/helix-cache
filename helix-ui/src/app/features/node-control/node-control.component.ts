import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { SectionTitleComponent } from "../../shared/components/section-title/section-title.component";
import { NodeColorPipe } from "../../shared/components/node-color.pipe";
import { ClusterStateService } from "../../core/services/cluster-state.service";
import { AdminApiService } from "../../core/services/admin-api.service";
import { environment } from "../../../environments/environment";
import { NodeStatusResponse } from "../../shared/interfaces/helix.interface";
import { ActivityLogComponent } from "../activity-log/activity-log.component";
import { forkJoin, Observable } from "rxjs";


@Component({
    selector: 'app-node-control',
    standalone: true,
    imports: [CommonModule, SectionTitleComponent, NodeColorPipe, ActivityLogComponent],
    templateUrl: './node-control.component.html',
    styleUrl: './node-control.component.css',
})
export class NodeControlComponent {
    protected readonly state = inject(ClusterStateService);
    private readonly admin = inject(AdminApiService);
    showPartitionModal = false;

    //nodeId -> 'A' | 'b' | null
    assignments: Record<string, 'A' | 'B' | null> = {};

    private nodeUrl(nodeId: string): string {
        return environment.nodes.find(n => n.id === nodeId)?.baseUrl ?? environment.nodes[0].baseUrl;
    }

    pause(node: NodeStatusResponse) { this.admin.pauseNode(this.nodeUrl(node.nodeId)).subscribe(); }
    resume(node: NodeStatusResponse) { this.admin.resumeNode(this.nodeUrl(node.nodeId)).subscribe(); }
    slow(node: NodeStatusResponse) { this.admin.slowNode(this.nodeUrl(node.nodeId), 3000).subscribe(); }

    openPartitionModal(): void {
        this.state.nodes().forEach(n => this.assignments[n.nodeId] = null);
        this.showPartitionModal = true;
    }

    closeModal(): void {
        this.showPartitionModal = false;
    }

    assignGroup(nodeId: string, group: 'A' | 'B'): void {
        this.assignments[nodeId] = this.assignments[nodeId] === group ? null : group;
    }

    get canCut(): boolean {
        const vals = Object.values(this.assignments);
        return vals.some(v => v === 'A') && vals.some(v => v === 'B');
    }

    splitEvenly(): void {
        const liveNodes = this.state.nodes().map(n => n.nodeId);
        liveNodes.forEach((id, i) => {
            this.assignments[id] = i%2 === 0 ? 'A' : 'B';
        });
    }

    cutNetwork(): void {
        const liveNodes = this.state.nodes().map(n => n.nodeId);
        const groupA = liveNodes.filter(id => this.assignments[id] === 'A');
        const groupB = liveNodes.filter(id => this.assignments[id] === 'B');
        const neutral = liveNodes.filter(id => !this.assignments[id]);

        //Each groupA nodes must block groupB nodes
        const calls: Array<Observable<any>> = [];
        groupA.forEach(id => {
            calls.push(this.admin.setPartition(this.nodeUrl(id), groupB));
        });
        groupB.forEach(id => {
            calls.push(this.admin.setPartition(this.nodeUrl(id), groupA));
        });

        //Neutral nodes get an empty block list (no restrictions)
        neutral.forEach(id => {
            calls.push(this.admin.setPartition(this.nodeUrl(id), []));
        })

        forkJoin(calls).subscribe(() => {
            this.state.setPartition({ 
                groupA: [...groupA, ...neutral], 
                groupB: [...groupB, ...neutral],
                explicitA: groupA,
                explicitB: groupB,
                neutral            
            });
            this.closeModal();
        })

    }

    mendNetwork(): void {
        const calls = this.state.nodes().map(n => this.admin.healPartition(this.nodeUrl(n.nodeId)));
        forkJoin(calls).subscribe(() => this.state.healPartition());
    }

    groupOf(nodeId: string): 'A' | 'B' | 'AB' | null {
        const p = this.state.partition();
        if (!p) return null;
        if (p.neutral.includes(nodeId)) return 'AB';
        if (p.explicitA.includes(nodeId)) return 'A';
        if (p.explicitB.includes(nodeId)) return 'B';
        return null;
    }
}