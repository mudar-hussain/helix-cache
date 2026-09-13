import {Component, inject, OnInit, OnDestroy, signal} from '@angular/core';
import { CommonModule} from '@angular/common';
import { interval, startWith, Subscription, switchMap} from 'rxjs';
import {ClusterApiService } from '../../core/services/cluster-api.service';
import {SectionTitleComponent} from '../../shared/components/section-title/section-title.component'; 
import {NodeColorPipe} from '../../shared/./../shared/components/node-color.pipe';
import { NodeDistributionResponse} from '../../shared/interfaces/helix.interface';
import {environment} from '../../../environments/environment';

@Component({
selector: 'app-distribution',
standalone: true,
imports: [CommonModule, SectionTitleComponent, NodeColorPipe],
templateUrl: './distribution.component.html',
styleUrl: './distribution.component.css',

})
export class DistributionComponent implements OnInit, OnDestroy {

    private readonly clusterApi = inject(ClusterApiService);
    private sub = new Subscription();
    readonly distribution = signal<NodeDistributionResponse []>([]);

    ngOnInit(): void {
        this.sub.add (
            interval (environment.pollIntervals.distribution)
            .pipe(
                startWith (0),
                switchMap(() => this.clusterApi.getDistribution())
            )
            .subscribe(data => this.distribution.set(data))
        );
    }

    ngOnDestroy(): void {
        this.sub.unsubscribe();
    }
}