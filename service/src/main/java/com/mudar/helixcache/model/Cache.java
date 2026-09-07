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
    private Timestamp expiresAt;
    private Timestamp lastAccessedAt;
    private long version = HelixConstant.DEFAULT_CACHE_VERSION;

    public Cache(String key, String value, String primaryNode, Timestamp createdAt, Timestamp expiresAt) {
        this.key = key;
        this.value = value;
        this.primaryNode = primaryNode;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
}
