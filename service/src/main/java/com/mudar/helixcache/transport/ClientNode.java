package com.mudar.helixcache.transport;

import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Cache;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class ClientNode {

    private final RestClient restClient;

    public String replicateCache(Node node, String key, String value, LocalDateTime expiresAt) {
        String baseUri = "http://" + node.address() + "/internal/cache/{key}";
        if(expiresAt != null) {
            return restClient.put()
                    .uri(baseUri + "?value={value}&expiresAt={expiresAt}",
                            key, value, expiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String body = new String(response.getBody().readAllBytes());
                        throw new HelixValidationException(body);
                    })
                    .body(String.class);
        } else {
            return restClient.put()
                    .uri(baseUri + "?value={value}", key, value)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String body = new String(response.getBody().readAllBytes());
                        throw new HelixValidationException(body);
                    })
                    .body(String.class);
        }
    }

    public Cache getCacheFromReplica(Node node, String key) {
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
}
