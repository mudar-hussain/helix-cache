package com.mudar.helixcache.cluster;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class NodeStateManager {

    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicLong slowDelayMs = new AtomicLong(0L);

    public void pause() {
        paused.set(true);
        log.warn("Node state set to PAUSED - ping endpoint will return 503");
    }

    public void resume() {
        paused.set(false);
        slowDelayMs.set(0L);
        log.info("Node state set to RESUMED - ping endpoint returning 200");
    }

    public void slow(long delayMs) {
        slowDelayMs.set(delayMs);
        log.warn("Node state set to SLOW - ping endpoint will delay {}ms", delayMs);
    }

    public boolean isPaused() {
        return paused.get();
    }

    public long getSlowDelayMs() {
        return slowDelayMs.get();
    }
}
