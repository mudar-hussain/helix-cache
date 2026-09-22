package com.mudar.helixcache.transport;

import com.mudar.helixcache.cluster.NodeStateManager;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import com.mudar.helixcache.model.Node;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClientNode {

    private final RestClient restClient;
    private final NodeStateManager nodeStateManager;

    public void validateNodeConnection(String nodeId) {
        if(nodeStateManager.isBlocked(nodeId)) {
            throw new HelixValidationException("Network partition: Outbound calls  to " + nodeId + " are blocked");
        }
    }

    public Cache replicateCache(Node node, String key, String value, Long ttlSeconds) {
        validateNodeConnection(node.id());
        String baseUri = "http://" + node.address() + "/internal/cache/{key}";
        if(ttlSeconds != null) {
            return restClient.put()
                    .uri(baseUri + "?value={value}&ttlSeconds={ttlSeconds}",
                            key, value, ttlSeconds)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String body = new String(response.getBody().readAllBytes());
                        throw new HelixValidationException(body);
                    })
                    .body(Cache.class);
        } else {
            return restClient.put()
                    .uri(baseUri + "?value={value}", key, value)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String body = new String(response.getBody().readAllBytes());
                        throw new HelixValidationException(body);
                    })
                    .body(Cache.class);
        }
    }

    public Cache getCacheFromReplica(Node node, String key) {
        validateNodeConnection(node.id());
        return restClient
                .get()
                .uri("http://" + node.address() + "/internal/cache/{key}", key)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                            String body = new String(response.getBody().readAllBytes());
                            throw new HelixValidationException(body);
                })
                .body(Cache.class);
    }

    public String deleteCacheFromReplica(Node node, String key) {
        validateNodeConnection(node.id());
        return restClient
                .delete()
                .uri("http://" + node.address() + "/internal/cache/{key}", key)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    String body = new String(response.getBody().readAllBytes());
                    throw new HelixValidationException(body);
                })
                .body(String.class);
    }

    public List<Cache> fetchCacheListForNode(Node node, String targetNodeId) {
        validateNodeConnection(node.id());
        return restClient
                .get()
                .uri("http://" + node.address() + "/internal/cache/sync/node?targetNodeId={targetNodeId}", targetNodeId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, ((request, response) -> {
                    String body = new String(response.getBody().readAllBytes());
                    throw new HelixValidationException(body);
                }))
                .body(new ParameterizedTypeReference<List<Cache>>() {});
    }
}
