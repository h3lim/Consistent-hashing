package com.example.consistent_hashing.controller;

import com.example.consistent_hashing.service.ShardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/shards")
public class ShardController {
    private static final Logger logger = LoggerFactory.getLogger(ShardController.class);
    private final ShardService shardService;


    public ShardController(ShardService shardService) {
        this.shardService = shardService;

    }

    @PostMapping("/remove/{shardId}")
    public ResponseEntity<String> removeShard(@PathVariable int shardId) {
        shardService.migrateUsersFromShard(shardId-1);
        shardService.getUserCountsInAllShards();
        return ResponseEntity.ok("Migration completed successfully");
    }

    @PostMapping("/add/{shardId}")
    public ResponseEntity<String> addShard(@PathVariable int shardId) {
        shardService.addShard(shardId-1);
        shardService.getUserCountsInAllShards();
        return ResponseEntity.ok("Shard added successfully");
    }

    @GetMapping("/cnt")
    public ResponseEntity<String> countDataInAllShards() {
        shardService.getUserCountsInAllShards();
        return ResponseEntity.ok("Counts in all shards");
    }
}

