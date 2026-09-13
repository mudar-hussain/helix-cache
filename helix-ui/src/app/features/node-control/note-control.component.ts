import { CommonModule } from "@angular/common";
import { Component, inject } from "@angular/core";
import { SectionTitleComponent } from "../../shared/components/section-title/section-title.component";
import { StatusBadgeComponent } from "../../shared/components/status-badge/status-badge.component";
import { NodeColorPipe } from "../../shared/components/node-color.pipe";
import { ClusterStateService } from "../../core/services/cluster-state.service";
import { AdminApiService } from "../../core/services/admin-api.service";
import { environment } from "../../../environments/environment";
import { NodeStatusResponse } from "../../shared/interfaces/helix.interface";


@Component({
    selector: 'app-node-control',
    standalone: true,
    imports: [CommonModule, SectionTitleComponent, StatusBadgeComponent, NodeColorPipe],
    templateUrl: './node-control.component.html',
    styleUrl: './node-control.component.css',
})
export class NodeControlComponent {
    protected readonly state = inject(ClusterStateService);
    private readonly admin = inject(AdminApiService);

    private nodeUrl(nodeId: string): string {
        return environment.nodes.find(n => n.id === nodeId)?.baseUrl ?? environment.primaryNode;
    }

    pause(node: NodeStatusResponse) { this.admin.pauseNode(this.nodeUrl(node.nodeId)).subscribe();}
    resume(node: NodeStatusResponse) { this.admin.resumeNode(this.nodeUrl(node.nodeId)).subscribe();}
    slow(node: NodeStatusResponse) { this.admin.slowNode(this.nodeUrl(node.nodeId), 3000).subscribe();}
}