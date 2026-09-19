import { Injectable } from "@angular/core";
import { CacheResponse, SeedRequestResult } from "../../shared/interfaces/helix.interface";
import { Observable } from "rxjs";
import { HttpClient, HttpParams } from "@angular/common/http";
import { ApiService } from "./api.service";

@Injectable({
    providedIn: 'root'
})
export class CacheApiService {

    constructor(private apiService: ApiService){}

    putCache(
        key: string,
        value: string,
        ttlSeconds?: number
    ): Observable<CacheResponse> {

        let params = new HttpParams().set('value', value);

        if (ttlSeconds !== undefined) {
            params = params.set('ttlSeconds', ttlSeconds.toString());
        }

        return this.apiService.withFallback<CacheResponse>(
            'put',
            `/cache/${encodeURIComponent(key)}`,
            {
                body: null,
                params
            }
        );
    }

    getCache(key: string): Observable<CacheResponse> {
        return this.apiService.withFallback<CacheResponse>(
            'get',
            `/cache/${encodeURIComponent(key)}`
        );
    }

    deleteCache(key: string): Observable<string> {
        return this.apiService.withFallback<string>(
            'delete',
            `/cache/${encodeURIComponent(key)}`
        );
    }

    seedCache(count: number): Observable<SeedRequestResult> {
        return this.apiService.withFallback<SeedRequestResult>(
            'put',
            `/cache/seed/${encodeURIComponent(count)}`
        );
    }
}