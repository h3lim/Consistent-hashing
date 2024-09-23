package com.example.consistent_hashing.hashing;

import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.hash.Hashing;

public class ConsistentHashing {

    private final int numberOfShards;
    private final int virtualNodes;
    private final SortedMap<Integer, Integer> circle = new TreeMap<>();

    public ConsistentHashing(int virtualNodes, int numberOfShards) {
        this.numberOfShards = numberOfShards;
        this.virtualNodes = virtualNodes;
        for (int i = 0; i < numberOfShards; i++) {
            addNode(i);
        }
    }


    private static int hash(long key) {
        return Hashing.murmur3_32().hashLong(key).asInt();
    }

    public void addNode(int shard) {
        for (int i = 0; i < virtualNodes; i++) {
            circle.put(hash((long) shard * 1000000 + i), shard);
        }
    }

    public void removeNode(int shard) {
        for (int i = 0; i < virtualNodes; i++) {
            circle.remove(hash((long) shard * 1000000 + i));
        }
    }

    public int getNode(long userId) {
        if (circle.isEmpty()) {
            throw new IllegalStateException("No shards available");
        }
        int hash = hash(userId);
        hash = hash & 0x7FFFFFFF;
        System.out.println(hash);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, Integer> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }
}
