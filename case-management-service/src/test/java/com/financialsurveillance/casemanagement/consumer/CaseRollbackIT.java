package com.financialsurveillance.casemanagement.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.casemanagement.AbstractIntegrationTest;
import com.financialsurveillance.casemanagement.producer.CaseEventProducer;
import com.financialsurveillance.casemanagement.repository.CaseRepository;
import com.financialsurveillance.casemanagement.repository.ProcessedAlertRepository;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.AlertStatus;
import com.financialsurveillance.events.CaseCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

public class CaseRollbackIT extends AbstractIntegrationTest {

    @MockitoBean
    private CaseEventProducer caseEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.alerts-persisted}")
    private String alertsTopic;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private ProcessedAlertRepository processedAlertRepository;

    @Test
    void shouldRollbackCase_whenEventPublishFails() throws ExecutionException, InterruptedException {

        AlertPersistedEvent event = AlertPersistedEvent.builder()
                .alertId(UUID.randomUUID())
                .alertTypeId("WASH_TRADE")
                .tradeId("TRD-1001")
                .advisorId("ADV-42")
                .ruleId("RULE-WT-01")
                .ruleName("Wash Trade Detection")
                .createdAt(ZonedDateTime.now())
                .persistedAt(ZonedDateTime.now())
                .violationDetails(objectMapper.createObjectNode())
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .build();

        //forcing kafka publish to fail
        doThrow(new RuntimeException("Kafka publish failed"))
                .when(caseEventProducer)
                .publishCaseCreated(any(CaseCreatedEvent.class));

        Map<String, Object> props = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "dlt-test", "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props, new StringDeserializer(),
                new StringDeserializer());
        consumer.subscribe(List.of(alertsTopic + ".DLT"));

        kafkaTemplate.send(alertsTopic, event.getAlertId().toString(), event).get();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var records = consumer.poll(Duration.ofMillis(500));
            assertFalse(records.isEmpty());
            assertEquals(event.getAlertId().toString(), records.iterator().next().key());
        });

        assertEquals(0, caseRepository.countByAlertId(event.getAlertId()));
        assertFalse(processedAlertRepository.existsById(event.getAlertId()));
        consumer.close();

    }
}
