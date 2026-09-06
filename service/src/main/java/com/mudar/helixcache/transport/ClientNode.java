package com.mudar.helixcache.transport;

import com.mudar.helixcache.cluster.Node;
import com.mudar.helixcache.model.Cache;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Component
public class ClientNode {

    public String addCache(Node node, String key, String value, LocalDateTime expiresAt) {
        return RestClient.create()
                .put()
                .uri("http://" + node.address()
                        + "/internal/cache/" + key
                        + "?value=" + value
                        + (expiresAt != null ? "&expiresAt=" + expiresAt : ""))
                .retrieve()
                .body(String.class);
    }

    public Cache getCache(Node node, String key) {
        return RestClient.create()
                .get()
                .uri("http://" + node.address()
                        + "/internal/cache/" + key)
                .retrieve()
                .body(Cache.class);
    }

    public String deleteCache(Node node, String key) {
        return RestClient.create()
                .delete()
                .uri("http://" + node.address()
                        + "/internal/cache/" + key)
                .retrieve()
                .body(String.class);
    }
}
