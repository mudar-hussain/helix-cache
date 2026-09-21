package com.mudar.helixcache.model;

import com.mudar.helixcache.utils.HelixConstant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
public class Cache {
    private String key;
    private String value;
    private String primaryNode;
    private Timestamp createdAt;
    private Long ttlSeconds;
    private Timestamp lastAccessedAt;
    private long version;

    public Cache(String key, String value, String primaryNode, Timestamp createdAt, Long ttlSeconds) {
        this.key = key;
        this.value = value;
        this.primaryNode = primaryNode;
        this.createdAt = createdAt;
        this.lastAccessedAt = createdAt;
        this.ttlSeconds = ttlSeconds;
        this.version = HelixConstant.DEFAULT_CACHE_VERSION;
    }

    public Cache(String key, String value, String primaryNode, Timestamp createdAt, Long ttlSeconds, long version) {
        this.key = key;
        this.value = value;
        this.primaryNode = primaryNode;
        this.createdAt = createdAt;
        this.lastAccessedAt = createdAt;
        this.ttlSeconds = ttlSeconds;
        this.version = version;
    }
}
