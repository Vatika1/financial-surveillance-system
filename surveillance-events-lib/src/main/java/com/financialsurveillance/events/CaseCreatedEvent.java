package com.financialsurveillance.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseCreatedEvent {
    private UUID caseId;

    private UUID alertId;

    private String advisorId;

    private CaseStatus status;       // always OPEN at creation

    private Instant createdAt;

}
