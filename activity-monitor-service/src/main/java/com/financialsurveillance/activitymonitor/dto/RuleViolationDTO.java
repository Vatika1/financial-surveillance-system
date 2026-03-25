package com.financialsurveillance.activitymonitor.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.financialsurveillance.events.AlertSeverity;
import lombok.*;

@Getter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleViolationDTO {

    private String ruleId;
    private String ruleName;
    private AlertSeverity severity;
    private String advisorId;
    private String tradeId;
    private String violationDescription;
    private JsonNode violationDetails;
}
