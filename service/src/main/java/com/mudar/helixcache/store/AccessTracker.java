package com.mudar.helixcache.store;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AccessTracker {

    private static final int BUCKET_COUNT = 6;              // 6 buckets x 10s = 60s window
    private static final int BUCKET_DURATION_MS = 10_000;

    // Per-key sliding window - array of 6 AtomicLong buckets
    private final ConcurrentHashMap<String, AtomicLong[]> accessMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> totalAccessMap = new ConcurrentHashMap<>();

    public void record(String key) {
        int bucket = currentBucket();
        AtomicLong[] accessBuckets = accessMap.computeIfAbsent(key, k-> {
            AtomicLong[] buckets = new AtomicLong[BUCKET_COUNT];
            for(int i = 0; i<BUCKET_COUNT; i++) buckets[i] = new AtomicLong(0);
            return buckets;
        });
        accessBuckets[bucket].incrementAndGet();
        totalAccessMap.merge(key, 1L, Long::sum);
    }

    public double getRecentRate(String key) {
        AtomicLong[] buckets = accessMap.get(key);
        if(buckets == null) return 0.0;
        long sum = 0;
        for(AtomicLong b: buckets) sum += b.get();
        // rate = access per second over the 60s window
        return (double) sum / (BUCKET_COUNT * BUCKET_DURATION_MS / 1000.0);
    }

    public long getTotalAccess(String key) {
        return totalAccessMap.getOrDefault(key, 0L);
    }

    public Collection<String> getTrackedKeys() {
        return accessMap.keySet();
    }

    // Called by scheduler every 10s to slide the window forward
    public void slideWindow() {
        int nextBucket = (currentBucket() + 1) % BUCKET_COUNT;
        accessMap.values().forEach(buckets -> buckets[nextBucket].set(0));
    }

    private int currentBucket() {
        return (int) ((System.currentTimeMillis() / BUCKET_DURATION_MS) % BUCKET_COUNT);
    }
}
