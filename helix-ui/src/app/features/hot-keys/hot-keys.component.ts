import { Component, inject, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { interval, startWith, Subscription, switchMap } from 'rxjs';
import { AdminApiService } from '../../core/services/admin-api.service';
import { SectionTitleComponent } from '../../shared/components/section-title/section-title.component';
import { HotKeyPredictionResponse } from '../../shared/interfaces/helix.interface';
import { environment } from '../../../environments/environment';

@Component({
    selector: 'app-hot-keys',
    standalone: true,
    imports: [CommonModule, SectionTitleComponent],
    templateUrl: './hot-keys.component.html',
    styleUrl: './hot-keys.component.css',

})
export class HotKeysComponent implements OnInit, OnDestroy {

    private sub = new Subscription();
    readonly predictions = signal<HotKeyPredictionResponse[]>([]);

    constructor(private adminApiService: AdminApiService) { }

    ngOnInit(): void {
        this.sub.add(
            interval(environment.pollIntervals.hotKeys)
                .pipe(
                    startWith(0),
                    switchMap(() => this.adminApiService.getPrediction())
                )
                .subscribe(data => this.predictions.set(data))
        );
    }

    ngOnDestroy(): void {
        this.sub.unsubscribe();
    }

    barWidth(emaScore: number): number {
        const threshold = environment.hotKeyThreshold;
        return Math.min(100, (emaScore / threshold) * 100);
    }

}