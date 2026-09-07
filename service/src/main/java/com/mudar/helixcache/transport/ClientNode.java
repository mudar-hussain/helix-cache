package com.mudar.helixcache.transport;

import com.mudar.helixcache.cluster.Node;
import com.mudar.helixcache.model.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ClientNode {

    private final RestClient restClient;

    public String addCache(Node node, String key, String value, LocalDateTime expiresAt) {
        return restClient
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
