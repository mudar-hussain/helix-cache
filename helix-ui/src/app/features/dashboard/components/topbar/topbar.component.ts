import { Component, inject } from "@angular/core";
import { StatCardComponent } from "../../../../shared/components/stat-card/stat-card.component";
import { RouterLink, RouterLinkActive } from "@angular/router";
import { CommonModule } from "@angular/common";
import { ClusterStateService } from "../../../../core/services/cluster-state.service";
import { environment } from "../../../../../environments/environment";

@Component({
    selector: 'app-topbar',
    standalone: true,
    imports: [CommonModule, StatCardComponent],
    templateUrl: './topbar.component.html',
    styleUrl: './topbar.component.css',
})
export class TopbarComponent {
    protected readonly state = inject(ClusterStateService);
    protected readonly N = environment.nodes.length;

    get replication() {
        return 3;
    }
    
}