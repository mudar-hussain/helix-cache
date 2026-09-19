
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ClusterStats, NodeDistributionResponse, NodeStatusResponse, RingNodeResponse, Node, ReplicaNodes } from "../../shared/interfaces/helix.interface";
import { ApiService } from "./api.service";

@Injectable({
  providedIn: 'root'
})
export class ClusterApiService {

    constructor(private apiService: ApiService) {}

    //Cluster API
    getNodes(): Observable<NodeStatusResponse[]> {
        return this.apiService.withFallback('get','/cluster/nodes');
    }

    getRing(): Observable<RingNodeResponse[]> {
        return this.apiService.withFallback('get','/cluster/ring');
    }

    getDistribution(): Observable<NodeDistributionResponse[]> {
        return this.apiService.withFallback('get','/cluster/distribution');
    }

    getStats(): Observable<ClusterStats> {
        return this.apiService.withFallback('get','/cluster/stats');
    }

    getRouteForKey(key: string): Observable<Node> {
        return this.apiService.withFallback('get',`/cluster/route/${encodeURIComponent(key)}`);
    }

    getReplicasForKey(key: string): Observable<ReplicaNodes> {
        return this.apiService.withFallback('get',`/cluster/replicas/${encodeURIComponent(key)}`);
    }

}