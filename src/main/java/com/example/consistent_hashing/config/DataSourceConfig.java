package com.example.consistent_hashing.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    private final ShardDataSourceProperties shardDataSourceProperties;

    public DataSourceConfig(ShardDataSourceProperties shardDataSourceProperties) {
        this.shardDataSourceProperties = shardDataSourceProperties;
    }
    @Bean
    public DataSource routingDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();

        int shardIndex = 0;
        for (ShardDataSourceProperties.Shard shard : shardDataSourceProperties.getShards()) {
            DataSource dataSource = DataSourceBuilder.create()
                    .url(shard.getUrl())
                    .username(shard.getUsername())
                    .password(shard.getPassword())
                    .driverClassName(shard.getDriverClassName())
                    .build();

            targetDataSources.put(shardIndex++, dataSource);
        }

        ShardRoutingDataSource routingDataSource = new ShardRoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(targetDataSources.get(0));

        return routingDataSource;
    }

    @Primary
    @Bean
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}
