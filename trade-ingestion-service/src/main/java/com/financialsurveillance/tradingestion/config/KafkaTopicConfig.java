package com.financialsurveillance.tradingestion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic tradesRawTopic() {
        return TopicBuilder.name("trades.raw")
                .partitions(3)
                .replicas(1)
                .build();
    }
}