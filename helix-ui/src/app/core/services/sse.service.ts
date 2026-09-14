import { Injectable } from "@angular/core";
import { environment } from "../../../environments/environment";
import { ClusterEvent } from "../../shared/interfaces/helix.interface";
import { Observable } from "rxjs";
import { ClusterEventType } from "../enums/helix.enum";


@Injectable({
  providedIn: 'root'
})
export class SseService {
    private readonly url = `${environment.primaryNode}/cluster/events/stream`;

    //Shared EventSource instance for the entire application
    readonly events$: Observable<ClusterEvent> = this.buildStream();

    private buildStream(): Observable<ClusterEvent> {
        return new Observable<ClusterEvent>(subscriber => {
            const eventSource = new EventSource(this.url);

            (Object.values(ClusterEventType) as string[]).forEach(eventType => {
                eventSource.addEventListener(eventType, (event: MessageEvent) => {
                    try {
                        const clusterEvent: ClusterEvent = JSON.parse(event.data);
                        subscriber.next(clusterEvent);
                    } catch (error) {
                        console.error("Error parsing SSE message:", error);
                    }
                });
            });

            eventSource.onerror = (error) => {
                console.error("SSE connection error: ", error);
            };

            return () => {
                eventSource.close();
            }
        });
    }

}