package com.example.consistent_hashing.controller;

import com.example.consistent_hashing.dto.MigrationStatistics;
import com.example.consistent_hashing.service.ShardService;
import com.example.consistent_hashing.dto.ShardDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ShardService shardService;

    public DashboardController(ShardService shardService) {
        this.shardService = shardService;
    }

    @GetMapping("/shard-distribution")
    public Map<Integer, Integer> getShardDistribution() {
        return shardService.getShardDistribution();
    }

    @GetMapping("/migration-statistics")
    public List<MigrationStatistics> getMigrationStatistics() {
        return shardService.getMigrationStatistics();
    }


    @GetMapping("/migrating-data")
    public Map<Integer, Integer> getMigratingData() {
        return shardService.getMigratingData();
    }

    @GetMapping("/new-assigned-data")
    public Map<Integer, Integer> getNewAssignedData() {
        return shardService.getNewAssignedData();
    }
}
