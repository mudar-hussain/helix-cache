import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { HotKeyPredictionResponse } from "../../shared/interfaces/helix.interface";
import { ApiService } from "./api.service";


@Injectable({
  providedIn: 'root'
})
export class AdminApiService {

    constructor(private http: HttpClient, private apiService: ApiService) {}

    pauseNode(nodeBaseUrl: string): Observable<any> {
        return this.http.put<unknown>(`${nodeBaseUrl}/admin/node/pause`, null);
    }

    resumeNode(nodeBaseUrl: string): Observable<any> {
        return this.http.put<unknown>(`${nodeBaseUrl}/admin/node/resume`, null);
    }

    slowNode(nodeBaseUrl: string, delayMs: number): Observable<any> {
        return this.http.post<unknown>(`${nodeBaseUrl}/admin/node/slow`, null,
            { params: new HttpParams().set('delayMs', delayMs.toString()) });
    }

    getPrediction(): Observable<HotKeyPredictionResponse[]> {
        return this.apiService.withFallback<HotKeyPredictionResponse[]>('get', '/admin/stats/predictions');
    }

    setPartition(nodeBaseUrl: string, blockedPeers: string[]): Observable<any> {
        return this.http.post<unknown>(`${nodeBaseUrl}/admin/node/partition`, { blockedPeers });
    }

    getPartition(nodeBaseUrl: string): Observable<{nodeId: string; blockedPeers: string[] }> {
        return this.http.get<{nodeId: string; blockedPeers: string[] }>(`${nodeBaseUrl}/admin/node/partition`);
    }

    healPartition(nodeBaseUrl: string): Observable<any> {
        return this.http.put<unknown>(`${nodeBaseUrl}/admin/node/heal`, null);
    }

}