package com.example.consistent_hashing.dto;

public class MigrationStatistics {
    private int shardFrom;
    private int shardTo;
    private int userCount;

    public MigrationStatistics(int shardFrom, int shardTo, int userCount) {
        this.shardFrom = shardFrom;
        this.shardTo = shardTo;
        this.userCount = userCount;
    }

    public int getShardFrom() {
        return shardFrom;
    }

    public void setShardFrom(int shardFrom) {
        this.shardFrom = shardFrom;
    }

    public int getShardTo() {
        return shardTo;
    }

    public void setShardTo(int shardTo) {
        this.shardTo = shardTo;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }
}