package com.financialsurveillance.activitymonitor.config;

import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.TradeCreatedEvent;
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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties, KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaProperties = kafkaProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Bean
    public ConsumerFactory<String, TradeCreatedEvent> tradeConsumerFactory(){
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "activity-monitor-service");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonDeserializer.class);

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeCreatedEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent> tradeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TradeCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 1️⃣ Plug in ConsumerFactory (how consumers are created)
        factory.setConsumerFactory(tradeConsumerFactory());

        // 2️⃣ Enable parallel consumption
        factory.setConcurrency(3);

        // 3️⃣ Manual acknowledgment (YOU control offset commit)
        factory.getContainerProperties().setAckMode(
                ContainerProperties.AckMode.MANUAL
        );

        // 4️⃣ Poll timeout (how long consumer waits for records)
        factory.getContainerProperties().setPollTimeout(3000);

        // 5️⃣ Error handling (retry + DLQ)
        factory.setCommonErrorHandler(kafkaErrorHandler());


        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {

        // 1️⃣ Sends failed messages to DLT
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate ,
                        (record, ex) -> new TopicPartition(
                                record.topic() + ".DLT",
                                record.partition()
                        )
                );

        // 2️⃣ Retry config → 3 retries, 2 sec delay
        FixedBackOff backOff = new FixedBackOff(2000L, 3);

        // 3️⃣ Main error handler
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, backOff);

        // 4️⃣ Optional: mark some exceptions as non-retryable
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class
        );

        return errorHandler;
    }

}
