package com.financialsurveillance.alertservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.AlertStatus;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDTO {

    private UUID alertId;
    private String alertTypeId;
    private String tradeId;
    private String advisorId;
    private String ruleId;
    private String ruleName;
    private AlertSeverity severity;
    private AlertStatus status;
    private ZonedDateTime createdAt;
    private JsonNode violationDetails;
}
