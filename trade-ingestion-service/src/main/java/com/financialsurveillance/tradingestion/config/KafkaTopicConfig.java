package com.financialsurveillance.tradingestion.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * NewTopic beans only create topics, they don't alter existing ones. If
 * trades.raw already exists in MSK with old settings (replicas=1,
 * min.insync.replicas=1), you must either: (a) recreate it via
 * kafka-configs.sh --alter, or (b) delete and let it recreate on next app
 * startup. Ephemeral MSK teardown/recreate cycles will pick up new settings
 * automatically.
 */
@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final short REPLICAS = 2;

    private final KafkaTopicsProperties topics;

    @Bean
    public NewTopic tradesRawTopic() {
        return TopicBuilder.name(topics.tradesRaw())
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
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
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }
}