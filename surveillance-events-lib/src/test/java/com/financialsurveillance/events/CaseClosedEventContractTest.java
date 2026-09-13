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

public class CaseClosedEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private CaseClosedEvent expectedEvent(){
        return CaseClosedEvent.builder()
                .caseId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .alertId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .advisorId("ADV-42")
                .finalStatus(CaseStatus.CLOSED_NO_ACTION)
                .closedBy("reviewer-1")
                .closedAt(ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    @Test
    void shouldSerializeToExpectedJson() throws Exception {
        CaseClosedEvent event = expectedEvent();
        String actualJson = objectMapper.writeValueAsString(event);

        String expectedJson = Files.readString(
                Path.of("src/test/resources/contracts/case-closed-event.json")
        );
        JsonNode actual = objectMapper.readTree(actualJson);
        JsonNode expected = objectMapper.readTree(expectedJson);

        assertEquals(expected, actual);
    }

    @Test
    void shouldDeserializeFromExpectedJson() throws Exception {
        String expectedJson = Files.readString(
                Path.of("src/test/resources/contracts/case-closed-event.json")
        );

        CaseClosedEvent actualEvent = objectMapper.readValue(expectedJson, CaseClosedEvent.class);
        CaseClosedEvent expectedEvent = expectedEvent();

        assertEquals(expectedEvent, actualEvent);
    }
}
