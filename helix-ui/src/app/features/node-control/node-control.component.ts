import { CommonModule } from "@angular/common";
import { Component, inject, signal } from "@angular/core";
import { SectionTitleComponent } from "../../shared/components/section-title/section-title.component";
import { NodeColorPipe } from "../../shared/components/node-color.pipe";
import { ClusterStateService } from "../../core/services/cluster-state.service";
import { AdminApiService } from "../../core/services/admin-api.service";
import { environment } from "../../../environments/environment";
import { ClusterEvent, NodeStatusResponse } from "../../shared/interfaces/helix.interface";
import { ClusterEventType } from "../../core/enums/helix.enum";


@Component({
    selector: 'app-node-control',
    standalone: true,
    imports: [CommonModule, SectionTitleComponent, NodeColorPipe],
    templateUrl: './node-control.component.html',
    styleUrl: './node-control.component.css',
})
export class NodeControlComponent {
    protected readonly state = inject(ClusterStateService);
    private readonly admin = inject(AdminApiService);

    readonly modalOpen = signal(false);
    readonly modalEvent = signal<ClusterEvent | null>(null);

    private nodeUrl(nodeId: string): string {
        return environment.nodes.find(n => n.id === nodeId)?.baseUrl ?? environment.nodes[0].baseUrl;
    }

    pause(node: NodeStatusResponse) { this.admin.pauseNode(this.nodeUrl(node.nodeId)).subscribe(); }
    resume(node: NodeStatusResponse) { this.admin.resumeNode(this.nodeUrl(node.nodeId)).subscribe(); }
    slow(node: NodeStatusResponse) { this.admin.slowNode(this.nodeUrl(node.nodeId), 3000).subscribe(); }

    openModal(clusterEvent: ClusterEvent): void {
        this.modalEvent.set(clusterEvent);
        this.modalOpen.set(true);
    }

    closeModal(): void {
        this.modalOpen.set(false);
    }

    eventColor(clusterEvent: ClusterEvent): string {
        const clusterEventType = clusterEvent.clusterEventType;
        if ([ClusterEventType.NODE_DOWN, ClusterEventType.REPLICA_FAILED, ClusterEventType.QUORUM_FAILED].includes(clusterEventType))
            return 'var(--color-danger)';
        if (clusterEventType === ClusterEventType.NODE_SUSPECT)
            return 'var(--color-warning)';
        if ([ClusterEventType.NODE_UP, ClusterEventType.QUORUM_SUCCESS, ClusterEventType.SYNC_COMPLETE].includes(clusterEventType))
            return 'var (--color-success)';
        return 'var (--color-accent)';
    }
}