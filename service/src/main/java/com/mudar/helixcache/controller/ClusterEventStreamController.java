package com.mudar.helixcache.controller;

import com.mudar.helixcache.model.ClusterEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/cluster/events")
public class ClusterEventStreamController {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L);  // no timeout
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    @Async
    @EventListener
    public void handleClusterEvent(ClusterEvent clusterEvent) {
        for (SseEmitter emitter : emitters) {
            sendEvent(emitter, clusterEvent);
        }
    }

    private void sendEvent(SseEmitter emitter,ClusterEvent clusterEvent) {
        try {
            emitter.send(SseEmitter.event()
                            .name(clusterEvent.getClusterEventType().name())
                            .data(clusterEvent)
            );
        } catch (IOException | IllegalStateException e) {
            // Client disconnected or emitter is already completed.
            emitters.remove(emitter);
            log.debug("Removing disconnected SSE client: {}",e.getMessage());
        }
    }
}
