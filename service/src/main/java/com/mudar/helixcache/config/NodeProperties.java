package com.mudar.helixcache.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "helix.node")
public class NodeProperties {
    private String id;
    private String host;
    private int port;
}
