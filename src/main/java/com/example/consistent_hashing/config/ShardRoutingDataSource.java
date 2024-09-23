package com.example.consistent_hashing.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class ShardRoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<Integer> currentShard = new ThreadLocal<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return currentShard.get();
    }

    public static void setCurrentShard(int shard) {
        currentShard.set(shard);
    }

    public static void clearCurrentShard() {
        currentShard.remove();
    }


    public static <T> T executeInShardContext(int shardId, ShardTask<T> task) {
        try {
            setCurrentShard(shardId);
            return task.execute();
        } finally {
            clearCurrentShard();
        }
    }

    @FunctionalInterface
    public interface ShardTask<T> {
        T execute();
    }
}
