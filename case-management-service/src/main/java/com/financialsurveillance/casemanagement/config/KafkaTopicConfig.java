package com.financialsurveillance.casemanagement.config;

import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.cases-created}")
    private String casesCreatedTopic;

    @Value("${kafka.topics.cases-closed}")
    private String casesClosedTopic;

    @Value("${kafka.topics.alerts-persisted}")
    private String alertsPersistedTopic;

    // Prod (MSK, 2 brokers) sets these to 2; local and test fall back to 1 for a single broker.
    @Value("${kafka.topics.replication-factor:1}")
    private short replicationFactor;

    @Value("${kafka.topics.min-insync-replicas:1}")
    private String minInsyncReplicas;

    @Bean
    public NewTopic casesCreatedTopic() {
        return topic(casesCreatedTopic);
    }

    @Bean
    public NewTopic casesClosedTopic() {
        return topic(casesClosedTopic);
    }

    // Dead letter topic for the topic this service consumes.
    // DeadLetterPublishingRecoverer routes to record.topic() + ".DLT", so the name must match.
    // NewTopic beans only create; they never alter an existing topic.
    @Bean
    public NewTopic alertsPersistedDltTopic() {
        return topic(alertsPersistedTopic + ".DLT");
    }

    private NewTopic topic(String name) {
        return new NewTopic(name, 3, replicationFactor)
                .configs(Map.of(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, minInsyncReplicas));
    }
}