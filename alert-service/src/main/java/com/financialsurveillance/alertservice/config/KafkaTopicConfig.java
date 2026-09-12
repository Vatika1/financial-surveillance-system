package com.financialsurveillance.alertservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Prod (MSK, 2 brokers) sets these to 2; local and test fall back to 1 for a single broker.
    @Value("${kafka.topics.replication-factor:1}")
    private int replicationFactor;

    @Value("${kafka.topics.min-insync-replicas:1}")
    private String minInsyncReplicas;

    @Bean
    public NewTopic alertsPersistedTopic() {
        return TopicBuilder.name("alerts.persisted")
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas)
                .build();
    }

    @Bean
    public NewTopic alertsCreatedDltTopic() {
        return TopicBuilder.name("alerts.created.DLT")
                .partitions(3)
                .replicas(replicationFactor)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas)
                .build();
    }
}