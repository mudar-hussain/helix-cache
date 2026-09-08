package com.mudar.helixcache.config;

import com.mudar.helixcache.model.Node;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "helix.cluster")
public class ClusterProperties {
    private int replicationFactor = 3;
    private int writeQuorum = 2;
    private int readQuorum = 2;
    private List<Node> nodes = new ArrayList<>();
}
