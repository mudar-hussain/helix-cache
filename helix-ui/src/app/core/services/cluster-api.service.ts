import { HttpClient } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { catchError, Observable } from "rxjs";
import { CacheStats, NodeDistributionResponse, NodeStatusResponse, RingNodeResponse, Node, ReplicaNodes } from "../../shared/interfaces/helix.interface";

@Injectable({
  providedIn: 'root'
})
export class ClusterApiService {
    
    private readonly http = inject(HttpClient);

    private withFallBack<T>(path: string): Observable<T> {
        const nodes = environment.nodes;
        return nodes.reduce((acc$, node, i) => {
            if (i===0) return this.http.get<T>(`${node.baseUrl}${path}`);
            return acc$.pipe(
                catchError(() => this.http.get<T>(`${node.baseUrl}${path}`))
            );
        }, null as any as Observable<T>);
    
    }

    //Cluster API
    getNodes(): Observable<NodeStatusResponse[]> {
        return this.withFallBack('/cluster/nodes');
    }

    getRing(): Observable<RingNodeResponse[]> {
        return this.withFallBack('/cluster/ring');
    }

    getDistribution(): Observable<NodeDistributionResponse[]> {
        return this.withFallBack('/cluster/distribution');
    }

    getStats(): Observable<CacheStats> {
        return this.withFallBack('cluster/stats');
    }

    getRouteForKey(key: string): Observable<Node> {
        return this.withFallBack(`cluster/route/${encodeURIComponent(key)}`);
    }

    getReplicasForKey(key: string): Observable<ReplicaNodes> {
        return this.withFallBack(`/cluster/replicas/${encodeURIComponent(key)}`);
    }

}