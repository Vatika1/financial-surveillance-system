package com.financialsurveillance.tradingestion.config;

import com.financialsurveillance.events.TradeCreatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaProperties kafkaProperties;

    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ProducerFactory<String, TradeCreatedEvent> producerFactory() {
        // Start with all Spring Kafka yml properties (includes SSL config in prod)
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());

        // Bootstrap servers (explicit for clarity, though yml provides them)
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializers
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Durability: wait for all replicas to acknowledge
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // Idempotent producer to prevent duplicate messages on retry
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Retry on transient failures
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Batch messages for better throughput
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}