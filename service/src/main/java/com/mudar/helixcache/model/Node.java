package com.mudar.helixcache.model;

public record Node(
        String id,
        String host,
        int port
){

    public String address() {
        return host + ":" + port;
    }
}
