package com.example.consistent_hashing.service;

import com.example.consistent_hashing.config.ShardDataSourceProperties;
import com.example.consistent_hashing.hashing.ConsistentHashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ConsistentHashingService {
    private static final Logger logger = LoggerFactory.getLogger(ConsistentHashingService.class);
    private final ConsistentHashing consistentHashing;
    private final Map<Integer, AtomicInteger> shardDistribution = new HashMap<>();
    private final Map<Integer, AtomicInteger> userCounts = new ConcurrentHashMap<>();

    public ConsistentHashingService(ShardDataSourceProperties shardDataSourceProperties) {
        int numOfShards = shardDataSourceProperties.getShards().size();
        int numOfVirtualNodes = 200;
        this.consistentHashing = new ConsistentHashing(numOfVirtualNodes, numOfShards);

        for (int i = 0; i < numOfShards; i++) {
            shardDistribution.put(i, new AtomicInteger(0));
            userCounts.put(i, new AtomicInteger(0));
        }
    }

    public ConsistentHashing getConsistentHashing() {
        return consistentHashing;
    }

    public int getShardForUser(Long userId) {
        int shard = consistentHashing.getNode(userId);
        shardDistribution.get(shard).incrementAndGet();
        logger.info("User {} assigned to shard {}", userId, shard);
        return shard;
    }

    public void addShard(int shardId) {
        consistentHashing.addNode(shardId);
        shardDistribution.put(shardId, new AtomicInteger(0));
        logger.info("Shard {} added to the consistent hash ring", shardId);
    }

}
