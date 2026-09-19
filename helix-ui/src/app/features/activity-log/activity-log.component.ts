import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClusterStateService } from '../../core/services/cluster-state.service';
import { ClusterEvent, ClusterEventEntry } from '../../shared/interfaces/helix.interface';
import { SectionTitleComponent } from '../../shared/components/section-title/section-title.component';
import { AutoScrollDirective } from '../../shared/directives/auto-scroll.directive';
import { ClusterEventType } from '../../core/enums/helix.enum';
import { environment } from '../../../environments/environment';
import { BADGE_COLOR, LOG_CATEGORIES } from '../../core/constants/app.constant';


@Component({
    selector: 'app-activity-log',
    standalone: true,
    imports: [CommonModule, SectionTitleComponent, AutoScrollDirective],
    templateUrl: './activity-log.component.html',
    styleUrl: './activity-log.component.css',
})
export class ActivityLogComponent {

    readonly nodeIds = ['ALL', ...environment.nodes.map(n => n.id)];
    readonly nodeColors = Object.fromEntries(environment.nodes.map(n => [n.id, n.color]));

    //Filter
    readonly categories = Object.keys(LOG_CATEGORIES);
    readonly activeCategory = signal<string>('ALL');
    readonly activeNode = signal<string>('ALL');

    //Modal state
    readonly modalOpen = signal(false);

    constructor(private clusterStateService: ClusterStateService, ) { }

    get events() {
        return this.clusterStateService.eventLog;
    }

    // Mini-log: always shows the 8 most-recent entries, no filter
    readonly miniEvents: any = computed((): any => this.clusterStateService.eventLog().slice(0, 8));

    // Modal log: applies both category + node filters
    readonly filteredEvents: any = computed((): any => {
        const types: ClusterEventType[] = LOG_CATEGORIES[this.activeCategory()];
        const nodeId: any = this.activeNode();
        return this.clusterStateService.eventLog().filter(entry => {
            const matchCat: boolean = !types?.length || types.includes(entry.event.clusterEventType);
            const matchNode: boolean = nodeId === 'ALL' || entry.event.nodeId === nodeId;
            return matchCat && matchNode;
        });
    });

    // - Actions
    openModal(): void { this.modalOpen.set(true); }
    closeModal(): void { this.modalOpen.set(false); }
    setCategory(cat: string): void { this.activeCategory.set(cat); }
    setNode(nodeId: string): void { this.activeNode.set(nodeId); }
    clear(): void { this.clusterStateService.eventLog.set([]); }

    //Display helpers
    badgeColor(entry: ClusterEventEntry): string {
        return BADGE_COLOR[entry.event.clusterEventType] ?? 'var(--color-accent)';
    }

    badgeLabel(entry: ClusterEventEntry): string {
        return entry.event.clusterEventType === ClusterEventType.CACHE_GET ? 'HOT_KEY' : entry.event.clusterEventType;
    }

    nodeColor(nodeId: string): string {
        return this.nodeColors[nodeId] ?? 'var(--color-accent)';
    }

}