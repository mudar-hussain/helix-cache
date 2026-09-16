import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { ClusterEvent } from "../../shared/interfaces/helix.interface";
import { distinctUntilChanged, merge, Observable, share } from "rxjs";
import { ClusterEventType } from "../enums/helix.enum";


@Injectable({
    providedIn: 'root'
})
export class SseService {
    // private readonly url = `${environment.primaryNode}/cluster/events/stream`;

    //Shared EventSource instance for the entire application
    // readonly events$: Observable<ClusterEvent> = this.buildStream().pipe(share());
    readonly events$: Observable<ClusterEvent> = merge(
        ...environment.nodes.map(node => this.nodeStream(node.baseUrl))
    ).pipe(
        distinctUntilChanged((a,b) => a.clusterEventType === b.clusterEventType
                                        && a.nodeId === b.nodeId
                                        && a.eventTimestamp === b.eventTimestamp),
        share());

    private nodeStream(baseUrl: string): Observable<ClusterEvent> {
        return new Observable<ClusterEvent>(subscriber => {
            const eventSource = new EventSource(`${baseUrl}/cluster/events/stream`);
            (Object.values(ClusterEventType) as string[]).forEach(eventType => {
                    eventSource.addEventListener(eventType, (event: MessageEvent) => {
                        try {
                            const clusterEvent: ClusterEvent = JSON.parse(event.data);
                            subscriber.next(clusterEvent);
                        } catch (error) {
                            console.error(`SSE parse error [${baseUrl}]:`, error);
                        }
                    });
                });

                eventSource.onerror = (error) => {
                    console.error(`SSE connection error on [${baseUrl}]: `, error);
                };

                return () => {
                    eventSource.close();
                }
        });
    }

}