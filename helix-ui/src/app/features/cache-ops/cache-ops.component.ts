import { CommonModule } from "@angular/common";
import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { SectionTitleComponent } from "../../shared/components/section-title/section-title.component";
import { CacheApiService } from "../../core/services/cache-api.service";
import { CacheResponse, Node, OpResult, SeedRequestResult } from "../../shared/interfaces/helix.interface";
import { ClusterApiService } from "../../core/services/cluster-api.service";
import { ClusterStateService } from "../../core/services/cluster-state.service";
import { HotKeysComponent } from "../hot-keys/hot-keys.component";


@Component({
    selector: 'app-cache-ops',
    standalone: true,
    imports: [CommonModule, FormsModule, SectionTitleComponent, HotKeysComponent],
    templateUrl: './cache-ops.component.html',
    styleUrl: './cache-ops.component.css',
})
export class CacheOpsComponent {
    private readonly cacheApi = inject(CacheApiService);
    private readonly clusterApi = inject(ClusterApiService);
    private readonly state = inject(ClusterStateService);

    writeKey = '';
    writeValue = '';
    writeTtl = '';
    readKey = '';
    seedKeys = '12';

    // ttlOptions = Object.keys(TtlOption);
    ttlOptions = ['never', '10s', '30s', '2m', '10m'];

    writeResult = signal<OpResult | null>(null);
    writeError = signal<OpResult | null>(null);
    readResult = signal<CacheResponse | null>(null);
    readError = signal<string | null>(null);
    deleteResult = signal<string | null>(null);
    seedLoading = signal(false);


    private ttlSeconds(ttl: string): number | undefined {
        return ({ '10s': 10, '30s': 30, '2m': 120, '10m': 600 } as Record<string, number>)[ttl];
    }

    // getValueByKey(value: string): number | undefined {
    //     var ttlOption = Object.entries(TtlOption).find(([key, val]) => key === value);
    //     if(ttlOption != undefined && ttlOption != null) {
    //         return ttlOption[1];
    //     } else {
    //         return -1;
    //     }
    // }

    onWrite(): void {
        if (!this.writeKey.trim() || !this.writeValue.trim()) return;
        this.resetResult();
        const t = Date.now();
        this.cacheApi.putCache(this.writeKey.trim(), this.writeValue.trim(), this.ttlSeconds(this.writeTtl)).subscribe({
            next: res => {
                this.writeResult.set({ latencyMs: Date.now() - t, body: res });

                // key exists - highlight primary node
                this.clusterApi.getReplicasForKey(this.writeKey.trim()).subscribe({
                    next: replica => this.state.setReplicaNodes(replica),
                    error: () => { }
                });
            },
            error: err => this.writeError.set({
                latencyMs: Date.now() - t,
                body: err.message
            }),
        });
    }

    onRead(): void {
        const key = this.readKey.trim();
        if (!key) return;
        this.resetResult();
        this.cacheApi.getCache(key).subscribe({
            next: res => {
                this.readResult.set(res);
                // key exists - highlight primary node
                this.clusterApi.getReplicasForKey(key).subscribe({
                    next: replica => this.state.setReplicaNodes(replica),
                    error: () => { }
                });
            },
            error: err => {
                if(err.status === 404) {
                    this.readError.set('Key not found');
                } else {
                    const msg = err.error?.message ?? err.error ?? err.message;
                    this.readError.set(msg);
                }
            }
        });
    }

    onDelete(): void {
        const key = this.readKey.trim();
        if (!key) return;
        this.resetResult();
        this.cacheApi.deleteCache(key).subscribe({
            next: res => { this.deleteResult.set(res); },
            error: err => {
                const msg = err.error?.message ?? err.error ?? err.message;
                if(msg.text === 'Cache entry removed successfully') {
                    this.deleteResult.set(msg.text);
                } else {
                    this.deleteResult.set('Error: ' + msg.text);
                }
            }
        });
    }

    resetResult(): void {
        this.writeResult.set(null);
        this.readResult.set(null);
        this.readError.set(null);
        this.deleteResult.set(null);
        this.state.clearReplicaNodes();
    }

    onSeedKeys(): void {
        const n = parseInt(this.seedKeys.trim(), 10);
        if (isNaN(n) || n <= 0) return;
        this.resetResult();
        this.seedLoading.set(true);

        this.cacheApi.seedCache(n).subscribe({
            next: () => {
                this.seedLoading.set(false);
                this.state.triggerRingBurst();
            },
            error: err => {
                this.seedLoading.set(false);
            }
        });
    }
}
