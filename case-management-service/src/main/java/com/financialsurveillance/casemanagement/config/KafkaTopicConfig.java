package com.financialsurveillance.casemanagement.config;

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

    @Bean
    public NewTopic casesCreatedTopic() {
        return new NewTopic(casesCreatedTopic, 3, (short) 2)
                .configs(java.util.Map.of(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2"));
    }

    @Bean
    public NewTopic casesClosedTopic() {
        return new NewTopic(casesClosedTopic, 3, (short) 2)
                .configs(java.util.Map.of(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2"));
    }

    // Dead letter topic for the topic this service consumes.
    // DeadLetterPublishingRecoverer routes to record.topic() + ".DLT", so the name must match.
    // NewTopic beans only create; they never alter an existing topic.
    @Bean
    public NewTopic alertsPersistedDltTopic() {
        return new NewTopic(alertsPersistedTopic + ".DLT", 3, (short) 2)
                .configs(java.util.Map.of(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2"));
    }
}