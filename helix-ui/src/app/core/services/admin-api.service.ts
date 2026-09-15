import { HttpClient, HttpParams } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { catchError, Observable } from "rxjs";
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
        return environment.nodes.slice(1).reduce(
            (acc$, node) => acc$.pipe(catchError(() => this.http.get<HotKeyPredictionResponse[]>(`${node.baseUrl}/admin/stats/predictions`))),
            this.http.get<HotKeyPredictionResponse[]>(`${environment.nodes[0].baseUrl}/admin/stats/predictions`)
        );
    }

}