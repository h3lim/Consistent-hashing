package com.example.consistent_hashing.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CustomIdGenerator {
    private static final long EPOCH = 1704067200000L;
    private static final int NODE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private final int nodeId;
    private final AtomicLong lastTimestamp = new AtomicLong(0L);
    private final AtomicLong sequence = new AtomicLong(0L);

    public CustomIdGenerator(int nodeId) {
        if (nodeId < 0 || nodeId >= (1 << NODE_BITS)) {
            throw new IllegalArgumentException("Node ID must be between 0 and " + ((1 << NODE_BITS) - 1));
        }
        this.nodeId = nodeId;
    }

    public synchronized long generateId() {
        long currentTimestamp = getCurrentTimestamp();

        if (currentTimestamp < lastTimestamp.get()) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " +
                    (lastTimestamp.get() - currentTimestamp) + " milliseconds");
        }

        if (currentTimestamp == lastTimestamp.get()) {
            long currentSequence = sequence.incrementAndGet() & ((1L << SEQUENCE_BITS) - 1);
            if (currentSequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence.set(0);
        }

        lastTimestamp.set(currentTimestamp);

        return ((currentTimestamp - EPOCH) << (NODE_BITS + SEQUENCE_BITS))
                | (nodeId << SEQUENCE_BITS)
                | sequence.get();
    }

    private long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp == lastTimestamp.get()) {
            currentTimestamp = getCurrentTimestamp();
        }
        return currentTimestamp;
    }
}

