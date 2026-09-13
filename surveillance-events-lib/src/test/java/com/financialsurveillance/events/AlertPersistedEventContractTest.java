package com.financialsurveillance.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlertPersistedEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AlertPersistedEvent expectedEvent() {
        return AlertPersistedEvent.builder()
                .alertId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .alertTypeId("WASH_TRADE")
                .tradeId("TRD-1001")
                .advisorId("ADV-42")
                .ruleId("RULE-WT-01")
                .ruleName("Wash Trade Detection")
                .createdAt(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .persistedAt(ZonedDateTime.of(2026, 1, 1, 0, 0, 1, 0, ZoneOffset.UTC))
                .violationDetails(objectMapper.createObjectNode().put("matchedTrades", 2))
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .build();
    }

    @Test
    void shouldSerializeToExpectedJson() throws Exception {
        AlertPersistedEvent event = expectedEvent();
        String actualJson = objectMapper.writeValueAsString(event);
        String expectedJson = Files.readString(
                Path.of("src/test/resources/contracts/alert-persisted-event.json")
        );

        JsonNode actual = objectMapper.readTree(actualJson);
        JsonNode expected = objectMapper.readTree(expectedJson);

        assertEquals(expected, actual);
    }

    @Test
    void shouldDeserializeFromExpectedJson() throws Exception {
        String expectedJson = Files.readString(
                Path.of("src/test/resources/contracts/alert-persisted-event.json")
        );
        AlertPersistedEvent actualEvent =  objectMapper.readValue(expectedJson, AlertPersistedEvent.class);
        AlertPersistedEvent expectedEvent = expectedEvent();

        assertEquals(expectedEvent, actualEvent);
    }
}
