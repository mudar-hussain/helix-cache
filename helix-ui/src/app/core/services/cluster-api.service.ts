import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Observable } from "rxjs";
import { CacheStats, NodeDistributionResponse, NodeStatusResponse, RingNodeResponse, Node } from "../../shared/interfaces/helix.interface";

@Injectable({
  providedIn: 'root'
})
export class ClusterApiService {
    
    private readonly http = inject(HttpClient);
    private readonly base = environment.primaryNode;

    //Cluster API
    getNodes(): Observable<NodeStatusResponse[]> {
        return this.http.get<NodeStatusResponse[]>(`${this.base}/cluster/nodes`);
    }

    getRing(): Observable<RingNodeResponse[]> {
        return this.http.get<RingNodeResponse[]>(`${this.base}/cluster/ring`);
    }

    getDistribution(): Observable<NodeDistributionResponse[]> {
        return this.http.get<NodeDistributionResponse[]>(`${this.base}/cluster/distribution`);
    }

    getStats(): Observable<CacheStats> {
        return this.http.get<CacheStats>(`${this.base}/cluster/stats`);
    }

    getRouteForKey(key: string): Observable<Node[]> {
        return this.http.get<Node[]>(`${this.base}/cluster/route/${encodeURIComponent(key)}`);
    }

    getReplicasForKey(key: string): Observable<Node[]> {
        return this.http.get<Node[]>(`${this.base}/cluster/replicas/${encodeURIComponent(key)}`);
    }


    

}