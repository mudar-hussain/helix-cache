package com.mudar.helixcache.cluster;

@FunctionalInterface
public interface HashFunction {
    Long hash(String value);
}
