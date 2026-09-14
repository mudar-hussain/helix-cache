import { inject, Injectable } from "@angular/core";
import { CacheResponse } from "../../shared/interfaces/helix.interface";
import { Observable } from "rxjs";
import { HttpClient, HttpParams } from "@angular/common/http";
import { environment } from "../../../environments/environment";

@Injectable({
  providedIn: 'root'
})
export class CacheApiService {
    
    private readonly http = inject(HttpClient);
    private readonly base = environment.primaryNode;

    putCache(key: string, value: string, ttlSeconds?: number): Observable<CacheResponse> {
        let params = new HttpParams().set('value', value);
        if (ttlSeconds !== undefined) {
            params = params.set('ttlSeconds', ttlSeconds.toString());
        }
        return this.http.put<CacheResponse>(`${this.base}/cache/${encodeURIComponent(key)}`, 
            null, 
            {headers: { 'Content-Type': 'text/plain' }, params }
        );
    }

    getCache(key: string): Observable<CacheResponse> {
        return this.http.get<CacheResponse>(`${this.base}/cache/${encodeURIComponent(key)}`);
    }

    deleteCache(key: string): Observable<string> {
        return this.http.delete<string>(`${this.base}/cache/${encodeURIComponent(key)}`);
    }





}