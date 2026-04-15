package com.financialsurveillance.casemanagement.dto;

import com.financialsurveillance.casemanagement.domain.ActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseActionResponse {
    private UUID id;

    private UUID caseId;

    private ActionType actionType;

    private String performedBy;

    private ZonedDateTime performedAt;

    private String fromValue;

    private String toValue;
}
