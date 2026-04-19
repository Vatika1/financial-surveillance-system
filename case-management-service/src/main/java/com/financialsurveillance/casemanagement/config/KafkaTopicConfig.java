package com.financialsurveillance.casemanagement.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.cases-created}")
    private String casesCreatedTopic;

    @Value("${kafka.topics.cases-closed}")
    private String casesClosedTopic;

    @Bean
    public NewTopic casesCreatedTopic() {
        return new NewTopic(casesCreatedTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic casesClosedTopic() {
        return new NewTopic(casesClosedTopic, 3, (short) 1);
    }
}