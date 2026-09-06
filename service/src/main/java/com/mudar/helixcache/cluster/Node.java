package com.mudar.helixcache.cluster;

public record Node(
        String id,
        String host,
        int port
){

    public String address() {
        return host + ":" + port;
    }
}
