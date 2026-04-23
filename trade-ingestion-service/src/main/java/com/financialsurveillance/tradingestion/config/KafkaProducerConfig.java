package com.financialsurveillance.tradingestion.config;

import com.financialsurveillance.events.TradeCreatedEvent;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Bean
    public ProducerFactory<String, TradeCreatedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Durability: wait for all replicas to acknowledge
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retry on transient failures
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Batch messages for better throughput
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);

        // Apply SSL config when security protocol is SSL (prod)
        // Keeps local dev (PLAINTEXT) working without SSL overhead
        if ("SSL".equalsIgnoreCase(securityProtocol)) {
            config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
            config.put(SslConfigs.SSL_ENABLED_PROTOCOLS_CONFIG, "TLSv1.2");
            config.put(SslConfigs.SSL_PROTOCOL_CONFIG, "TLSv1.2");
            // Hostname verification stays at default "https" (industry standard)
        }

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, TradeCreatedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}