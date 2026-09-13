package com.financialsurveillance.casemanagement.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.casemanagement.AbstractIntegrationTest;
import com.financialsurveillance.casemanagement.repository.CaseRepository;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.AlertStatus;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
public class AlertPersistedEventConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${kafka.topics.alerts-persisted}")
    private String alertsTopic;


    private AlertPersistedEvent alertEvent(UUID alertId) {
        return AlertPersistedEvent.builder()
                .alertId(alertId)
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

    }

    @Test
    void shouldCreateCase_whenAlertEventConsumed() throws ExecutionException, InterruptedException {
        UUID alertId = UUID.randomUUID();
        AlertPersistedEvent alertPersistedEvent = alertEvent(alertId);

        kafkaTemplate.send(alertsTopic, alertPersistedEvent.getAlertId().toString(), alertPersistedEvent).get();

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertTrue(caseRepository.existsByAlertId(alertId));
                });
    }

    @Test
    void shouldSkipCase_whenDuplicateAlertConsumed(CapturedOutput output) throws ExecutionException, InterruptedException {
        UUID alertId = UUID.randomUUID();
        AlertPersistedEvent alertPersistedEvent = alertEvent(alertId);

        kafkaTemplate.send(alertsTopic, alertPersistedEvent.getAlertId().toString(), alertPersistedEvent).get();

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertEquals(1, caseRepository.countByAlertId(alertId))
                );

        kafkaTemplate.send(alertsTopic, alertPersistedEvent.getAlertId().toString(), alertPersistedEvent).get();

        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() ->
                        assertTrue(output.getOut().contains("Duplicate alert detected"))
                );

        assertEquals(1, caseRepository.countByAlertId(alertId));
        assertEquals(1, StringUtils.countOccurrencesOf(output.getOut(), "Creating case for Alert"));
    }

    @Test
    void shouldRouteToDlt_whenMessageIsPoison() throws ExecutionException, InterruptedException {
        long before = caseRepository.count();

        Map<String, Object> props = KafkaTestUtils.consumerProps(kafka.getBootstrapServers(), "dlt-test", "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props, new StringDeserializer(),
                new StringDeserializer());
        consumer.subscribe(List.of(alertsTopic + ".DLT"));

        AlertPersistedEvent alertPersistedEvent = alertEvent(null);

        kafkaTemplate.send(alertsTopic, "poison", alertPersistedEvent).get();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            var records = consumer.poll(Duration.ofMillis(500));
            assertFalse(records.isEmpty());
            assertEquals("poison", records.iterator().next().key());
        });

        assertEquals(before, caseRepository.count());
        consumer.close();
    }

}
