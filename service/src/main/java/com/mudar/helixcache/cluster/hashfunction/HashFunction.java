package com.mudar.helixcache.cluster.hashfunction;

@FunctionalInterface
public interface HashFunction {
    Long hash(String value);
}
