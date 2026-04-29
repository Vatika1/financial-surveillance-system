package com.financialsurveillance.tradingestion.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 1;

    private final KafkaTopicsProperties topics;

    @Bean
    public NewTopic tradesRawTopic() {
        return TopicBuilder.name(topics.tradesRaw())
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }

    @Bean
    public NewTopic tradesRawDltTopic() {
        // DLT mirrors the source topic's partition/replica config so messages
        // routed here on poison-pill failures preserve key-based partitioning.
        // Activity-monitor is the writer; trade-ingestion declares it so all
        // trades.raw.* topics are owned in one place.
        return TopicBuilder.name(topics.tradesRawDlt())
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}