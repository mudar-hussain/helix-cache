package com.mudar.helixcache.dto;

import java.time.LocalDateTime;

public record Hint (
        String key,
        String value,
        LocalDateTime expiresAt,
        String targetNodeId,
        String targetAddress,
        LocalDateTime createdAt
) {}
