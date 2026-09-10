package com.mudar.helixcache.store;

import com.mudar.helixcache.dto.Hint;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HintedHandOffStore {

    private final Map<String, List<Hint>> hints = new ConcurrentHashMap<>();

    public void add(Hint hint) {
        hints.computeIfAbsent(hint.targetNodeId(), k ->
                Collections.synchronizedList(new ArrayList<>())).add(hint);
    }

    public List<Hint> getHintsForNode(String nodeId) {
        return hints.getOrDefault(nodeId, Collections.emptyList());
    }

    public void removeHint(String nodeId, Hint hint) {
        List<Hint> queue = hints.get(nodeId);
        if(queue!=null) queue.remove(hint);
    }

    public Map<String, Integer> getQueueDepths() {
        Map<String, Integer> depth = new HashMap<>();
        hints.forEach((nodeId, queue) -> depth.put(nodeId, queue.size()));
        return depth;
    }

    public int totalSize() {
        return hints.values().stream().mapToInt(List::size).sum();
    }
}
