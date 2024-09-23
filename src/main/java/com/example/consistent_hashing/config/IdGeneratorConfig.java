package com.example.consistent_hashing.config;

import com.example.consistent_hashing.util.CustomIdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdGeneratorConfig {

    @Value("${app.id-generator.node-id}")
    private int nodeId;

    @Bean
    public CustomIdGenerator customIdGenerator() {
        return new CustomIdGenerator(nodeId);
    }
}