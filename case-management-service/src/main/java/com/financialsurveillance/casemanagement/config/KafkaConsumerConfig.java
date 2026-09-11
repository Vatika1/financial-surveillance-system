package com.financialsurveillance.casemanagement.config;

import com.financialsurveillance.events.AlertPersistedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Bean
    public ConsumerFactory<String, AlertPersistedEvent> caseConsumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "case-management-service");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Chunk 2: wrap deserializers so a malformed record becomes a DeserializationException
        // handed to the error handler instead of a poison pill that blocks the partition.
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AlertPersistedEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        DefaultKafkaConsumerFactory<String, AlertPersistedEvent> factory =
                new DefaultKafkaConsumerFactory<>(props);
        factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AlertPersistedEvent> caseKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AlertPersistedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(caseConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.setCommonErrorHandler(kafkaErrorHandler());

        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) -> new TopicPartition(
                                record.topic() + ".DLT",
                                record.partition()
                        )
                );

        // Chunk 3: 1s, 2s, 4s (capped at 10s), 3 retries total, then route to DLT.
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxInterval(10000L);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Retrying these can never succeed — straight to DLT.
        errorHandler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );

        return errorHandler;
    }
}