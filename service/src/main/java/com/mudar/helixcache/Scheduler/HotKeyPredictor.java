package com.mudar.helixcache.Scheduler;

import com.mudar.helixcache.cluster.ClusterEventPublisher;
import com.mudar.helixcache.dto.HotKeyPredictionResponse;
import com.mudar.helixcache.enums.ClusterEventType;
import com.mudar.helixcache.store.AccessTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotKeyPredictor {

    private static final double ALPHA = 0.4;            //EMA smoothing factor
    private static final double HOT_THRESHOLD = 2.0;    // accesses/sec to flag as HOT

    private final AccessTracker accessTracker;
    private final ClusterEventPublisher clusterEventPublisher;

    // Persists EMA score across scheduler runs
    private final Map<String, Double> emaScores = new ConcurrentHashMap<>();
    private final List<HotKeyPredictionResponse> lastPredictions = new ArrayList<>();

    @Scheduled(fixedDelay = 10000)
    public void predict() {
        accessTracker.slideWindow();

        List<HotKeyPredictionResponse> predictionResponseList = new ArrayList<>();

        for (String key: accessTracker.getTrackedKeys()) {
            double currentRate = accessTracker.getRecentRate(key);
            double prevEma = emaScores.getOrDefault(key, currentRate);
            double newEma = ALPHA * currentRate + (1 - ALPHA) * prevEma;
            emaScores.put(key, newEma);

            boolean predicted = newEma >= HOT_THRESHOLD;
            predictionResponseList.add(new HotKeyPredictionResponse(key, newEma, currentRate, accessTracker.getTotalAccess(key), predicted));

            if(predicted) {
                log.info("Hot key predicted: '{}' EMA={}", key, String.format("%.2f", newEma));
                clusterEventPublisher.publish(ClusterEventType.CACHE_GET, "local", key,
                "Hot key predicted - EMA rate: " + String.format("%.2f", newEma) + " req/s", "WARN");
            }
        }

        //Sort hottest first
        predictionResponseList.sort((a,b) -> Double.compare(b.emaScore(), a.emaScore()));
        synchronized (lastPredictions) {
            lastPredictions.clear();
            lastPredictions.addAll(predictionResponseList);
        }
    }

    public List<HotKeyPredictionResponse> getLastPredictions() {
        synchronized (lastPredictions) {
            return new ArrayList<>(lastPredictions);
        }
    }

    public double getEmaScoreForKey(String key) {
        return this.getLastPredictions().stream()
                .filter(p -> p.key().equals(key))
                .mapToDouble(HotKeyPredictionResponse::emaScore)
                .findFirst().orElse(0.0);
    }
}
