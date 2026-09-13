import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ClusterStateService } from '../../core/services/cluster-state.service';
import { ClusterEvent } from '../../shared/interfaces/helix.interface';
import { SectionTitleComponent } from '../../shared/components/section-title/section-title.component';
import { AutoScrollDirective } from '../../shared/directives/auto-scroll.directive';
import { ClusterEventType } from '../../core/enums/helix.enum';

const MAX_EVENTS = 200;

@Component({
    selector: 'app-activity-log',
    standalone: true,
    imports: [CommonModule, SectionTitleComponent, AutoScrollDirective],
    templateUrl: './activity-log.component.html',
    styleUrl: './activity-log.component.css',
})
export class ActivityLogComponent implements OnInit, OnDestroy {

    // private readonly clusterStateService = inject(ClusterStateService);

    private sub = new Subscription();

    readonly events = signal<ClusterEvent []>([]);

    constructor(private clusterStateService: ClusterStateService) {}

    ngOnInit(): void {
        this.sub.add(
            this.clusterStateService.events$.subscribe(ev => {
                this.events.update (prev => [ev, ... prev].slice(0, MAX_EVENTS));
            })
        );
    }

    ngOnDestroy(): void {
        this.sub.unsubscribe();
    }

    clear(): void {
        this.events.set([]);
    }

    eventColor (ev: ClusterEvent): string {
        const t = ev.clusterEventType;
        if (t === ClusterEventType.NODE_DOWN || t === ClusterEventType.REPLICA_FAILED || t === ClusterEventType.QUORUM_FAILED) {
            return 'var(--color-danger)';
        }
        if (t === ClusterEventType.NODE_SUSPECT) {
            return 'var(--color-warning)';
        }
        if (t === ClusterEventType.NODE_UP || t === ClusterEventType.QUORUM_SUCCESS || t === ClusterEventType.SYNC_COMPLETE) {
            return 'var(--color-success)';
        }
        return 'var (--color-accent)';
    }

}