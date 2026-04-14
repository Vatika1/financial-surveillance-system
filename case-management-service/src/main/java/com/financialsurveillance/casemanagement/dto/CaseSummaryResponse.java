package com.financialsurveillance.casemanagement.dto;

import com.financialsurveillance.events.CaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseSummaryResponse {

    private UUID id;

    private UUID alertId;

    private String advisorId;

    private CaseStatus status;

    private String assignedTo;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant closedAt;
}
