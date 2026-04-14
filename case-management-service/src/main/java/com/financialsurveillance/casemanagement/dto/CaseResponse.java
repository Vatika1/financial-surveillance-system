package com.financialsurveillance.casemanagement.dto;

import com.financialsurveillance.events.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseResponse {
    private UUID id;

    private UUID alertId;

    private String advisorId;

    private CaseStatus status;

    private String assignedTo;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant closedAt;

    private List<CaseActionResponse> auditTrails;
}
