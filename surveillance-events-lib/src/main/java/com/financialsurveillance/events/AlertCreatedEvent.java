package com.financialsurveillance.events;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertCreatedEvent {

    private UUID alertId;
    private String tradeId;
    private String advisorId;
    private String ruleId;
    private String ruleName;
    private ZonedDateTime createdAt;
    private JsonNode violationDetails;
    private AlertSeverity severity;
    private AlertStatus status;
}
