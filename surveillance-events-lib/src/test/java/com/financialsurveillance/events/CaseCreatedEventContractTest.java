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

public class CaseCreatedEventContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    private CaseCreatedEvent expectedEvent(){
        return CaseCreatedEvent.builder()
                .caseId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .alertId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .advisorId("ADV-42")
                .status(CaseStatus.OPEN)
                .createdAt(ZonedDateTime.of(2026, 1, 1, 0, 0, 2, 0, ZoneOffset.UTC))
                .build();
    }

    @Test
    void shouldSerializeToExpectedJson() throws Exception {
        CaseCreatedEvent event = expectedEvent();
        String actualJson = objectMapper.writeValueAsString(event);

        String expectedJson = Files.readString(
                Path.of("src/test/resources/contracts/case-created-event.json")
        );
        JsonNode actual = objectMapper.readTree(actualJson);
        JsonNode expected = objectMapper.readTree(expectedJson);

        assertEquals(expected, actual);
    }

    @Test
    void shouldDeserializeFromExpectedJson() throws Exception {
        String expectedJson = Files.readString(
                Path.of("src/test/resources/contracts/case-created-event.json")
        );

        CaseCreatedEvent actualEvent = objectMapper.readValue(expectedJson, CaseCreatedEvent.class);
        CaseCreatedEvent expectedEvent = expectedEvent();

        assertEquals(expectedEvent, actualEvent);
    }
}
