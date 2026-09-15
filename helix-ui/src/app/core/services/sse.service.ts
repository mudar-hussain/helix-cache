import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { ClusterEvent } from "../../shared/interfaces/helix.interface";
import { merge, Observable, share } from "rxjs";
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
    ).pipe(share());

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

    private buildStream(): Observable<ClusterEvent> {
        const perNode$ = environment.nodes.map(node =>
            new Observable<ClusterEvent>(subscriber => {
                const eventSource = new EventSource(`${node.baseUrl}/cluster/events/stream`);
                (Object.values(ClusterEventType) as string[]).forEach(eventType => {
                    eventSource.addEventListener(eventType, (event: MessageEvent) => {
                        try {
                            const clusterEvent: ClusterEvent = JSON.parse(event.data);
                            subscriber.next(clusterEvent);
                        } catch (error) {
                            console.error(`SSE parse error [${node.id}]:`, error);
                        }
                    });
                });

                eventSource.onerror = (error) => {
                    console.error(`SSE connection error on [${node.id}]: `, error);
                };

                return () => {
                    eventSource.close();
                }
            })
        );

        // merge to all 
        return merge(...perNode$).pipe(share());
    }

}