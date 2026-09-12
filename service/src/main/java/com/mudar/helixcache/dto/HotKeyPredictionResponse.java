package com.mudar.helixcache.dto;

public record HotKeyPredictionResponse (
        String key,
        double emaScore,
        double recentRate,
        long totalAccess,
        boolean predicted
) {}
