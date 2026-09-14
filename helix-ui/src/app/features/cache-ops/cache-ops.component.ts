import { CommonModule } from "@angular/common";
import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { SectionTitleComponent } from "../../shared/components/section-title/section-title.component";
import { CacheApiService } from "../../core/services/cache-api.service";
import { TtlOption } from "../../core/enums/helix.enum";
import { CacheResponse, OpResult } from "../../shared/interfaces/helix.interface";


@Component({
    selector: 'app-cache-ops',
    standalone: true,
    imports: [CommonModule, FormsModule, SectionTitleComponent],
    templateUrl: './cache-ops.component.html',
    styleUrl: './cache-ops.component.css',
})
export class CacheOpsComponent {
    private readonly cacheApi = inject(CacheApiService);
    
    writeKey = '';
    writeValue = '';
    writeTtl = '';
    readKey = '';
    // ttlOptions = Object.keys(TtlOption);
    ttlOptions = ['never', '10s', '30s', '2m', '10m'];

    writeResult = signal<OpResult | null>(null);
    readResult = signal<CacheResponse | null>(null);
    readError = signal<string | null>(null);
    deleteResult = signal<string | null>(null);

    private ttlSeconds(ttl: string): number | undefined {
        return ({ '10s': 10, '30s': 30, '2m': 120, '10m': 600} as Record<string, number>)[ttl];
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
        const t = Date.now();
        this.cacheApi.putCache(this.writeKey.trim(), this.writeValue.trim(), this.ttlSeconds(this.writeTtl)).subscribe({
                next: res => this.writeResult.set({ status: 'ok', statusCode: 200, latencyMs: Date.now() - t, body:
                JSON.stringify(res, null, 2), primaryNode: res.primaryNode?.id }),
                error: err => this.writeResult.set({ status: 'error', statusCode: err.status ?? 0, latencyMs: Date.now() - t, body:
                err.message }),
        });
    }

    onRead():void {
        if (!this.readKey.trim()) return;
        this.readResult.set(null); this.readError.set(null);
        this.cacheApi.getCache (this.readKey.trim()).subscribe ({
            next: res => this.readResult.set (res),
            error: err => this.readError.set(err.status === 404? 'Key not found' : err.message),
        });
    }

    onDelete(): void {
        if (!this.readKey.trim()) return;
        this.deleteResult.set(null);
        this.cacheApi.deleteCache(this.readKey.trim()).subscribe({
            next: () => { this.deleteResult.set('Deleted'); this.readResult.set(null); },
            error: err => this.deleteResult.set('Error: ' + err.message),
        });
    }

}