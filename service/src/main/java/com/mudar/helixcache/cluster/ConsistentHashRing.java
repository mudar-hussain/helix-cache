package com.mudar.helixcache.cluster;

import com.mudar.helixcache.exception.HelixValidationException;
import com.mudar.helixcache.model.Node;
import com.mudar.helixcache.model.VirtualNode;
import com.mudar.helixcache.utils.HelixUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
@Setter
@NoArgsConstructor
public class ConsistentHashRing {
    private int virtualNodesPerNode = 3;
    private HashFunction hashFunction = new Sha256HashFunction();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Node> nodeMap = new ConcurrentHashMap<>();
    private final NavigableMap<Long, VirtualNode> ring = new TreeMap<>();

    public ConsistentHashRing(int virtualNodesPerNode, HashFunction hashFunction) {
        if(virtualNodesPerNode <= 0) {
            throw new IllegalArgumentException("VirtualNodesPerNode must be greater than zero");
        }
        this.virtualNodesPerNode = virtualNodesPerNode;
        this.hashFunction = hashFunction;
    }

    public void addNode(Node node) {
        lock.writeLock().lock();
        try {
            if(containsNode(node.id())) return;
            nodeMap.put(node.id(), node);
            for(int i = 0; i<this.virtualNodesPerNode; i++) {
                String virtualNodeKey = node.id() + "#" + i;
                Long hash = hashFunction.hash(virtualNodeKey);
                ring.put(hash, new VirtualNode(node, i, hash));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeNode(String nodeId) {
        lock.writeLock().lock();
        try {
            Node node = nodeMap.remove(nodeId);
            if(node == null) return;
            for(int i = 0; i<this.virtualNodesPerNode; i++) {
                String virtualNodeKey = node.id() + "#" + i;
                Long hash = hashFunction.hash(virtualNodeKey);
                ring.remove(hash);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Node getNode(String key) {
        HelixUtils.validateKey(key);
        lock.readLock().lock();
        try{
            if(ring.isEmpty()) {
                throw new HelixValidationException("No Nodes Available");
            }
            Long hash = hashFunction.hash(key);
            Map.Entry<Long, VirtualNode> ringEntry = ring.ceilingEntry(hash);
            if(ringEntry == null) {
                ringEntry = ring.firstEntry();
            }
            return ringEntry.getValue().node();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsNode(String nodeId) {
        return nodeMap.containsKey(nodeId);
    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    public int getVirtualNodeCount() {
        return ring.size();
    }

    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(
                new ArrayList<>(nodeMap.values())
        );
    }

    public List<VirtualNode> getVirtualNodes() {
        lock.readLock().lock();
        try{
            return Collections.unmodifiableList(
                    new ArrayList<>(ring.values())
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Node> getReplicaNodes(String key, int n) {
        HelixUtils.validateKey(key);
        lock.readLock().lock();
        try {
            if(ring.isEmpty()) {
                throw new HelixValidationException("No Nodes Available");
            }
            List<Node> replicas = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();

            Long hash = hashFunction.hash(key);

            //Start from the ceiling entry, walk the full ring if needed
            NavigableMap<Long, VirtualNode> tailMap = ring.tailMap(hash, true);
            for(VirtualNode v: tailMap.values()) {
                if(seen.add(v.node().id())) {
                    replicas.add(v.node());
                }
                if(replicas.size() == n) return replicas;
            }

            //Wrap around from the beginning of the ring
            for(VirtualNode v: ring.values()) {
                if(seen.add(v.node().id())) {
                    replicas.add(v.node());
                }
                if(replicas.size() == n) return replicas;
            }
            return replicas;
        } finally {
            lock.readLock().unlock();
        }
    }

}
