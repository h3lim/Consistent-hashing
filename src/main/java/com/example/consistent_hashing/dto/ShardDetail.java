package com.example.consistent_hashing.dto;

public class ShardDetail {
    private int shardId;
    private int userCount;

    public ShardDetail(int shardId, int userCount) {
        this.shardId = shardId;
        this.userCount = userCount;
    }

    public int getShardId() {
        return shardId;
    }

    public void setShardId(int shardId) {
        this.shardId = shardId;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }
}
