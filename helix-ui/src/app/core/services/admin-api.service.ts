import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { HotKeyPredictionResponse } from "../../shared/interfaces/helix.interface";
import { environment } from "../../../environments/environment";


@Injectable({
  providedIn: 'root'
})
export class AdminApiService {
    private readonly http = inject(HttpClient);
    private readonly base = environment.primaryNode;

    pauseNode(nodeBaseUrl: string): Observable<unknown> {
        return this.http.put<unknown>(`${nodeBaseUrl}/admin/node/pause`, null);
    }

    resumeNode(nodeBaseUrl: string): Observable<unknown> {
        return this.http.put<unknown>(`${nodeBaseUrl}/admin/node/resume`, null);
    }

    slowNode(nodeBaseUrl: string, delayMs: number): Observable<unknown> {
        return this.http.post<unknown>(`${nodeBaseUrl}/admin/node/slow`, null,
            { params: new HttpParams().set('delayMs', delayMs.toString()) });
    }

    getPrediction(): Observable<HotKeyPredictionResponse[]> {
        return this.http.get<HotKeyPredictionResponse[]>(`${this.base}/admin/stats/predictions`);
    }

}