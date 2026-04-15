package com.financialsurveillance.casemanagement.dto;

import com.financialsurveillance.events.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDetailResponse {
    private UUID id;

    private UUID alertId;

    private String advisorId;

    private CaseStatus status;

    private String assignedTo;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;

    private ZonedDateTime closedAt;

    private List<CaseActionResponse> auditTrails;
}
