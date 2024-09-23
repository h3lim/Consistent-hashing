package com.example.consistent_hashing.controller;

import com.example.consistent_hashing.service.ShardService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ShardService shardService;

    public WebSocketController(SimpMessagingTemplate messagingTemplate, ShardService shardService) {
        this.messagingTemplate = messagingTemplate;
        this.shardService = shardService;
    }

    @Scheduled(fixedRate = 3000)
    public void sendShardUpdates() {
        messagingTemplate.convertAndSend("/topic/shard-distribution", shardService.getShardDistribution());
    }

}