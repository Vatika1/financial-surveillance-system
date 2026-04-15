package com.financialsurveillance.events;

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
public class CaseClosedEvent {

    private UUID caseId;

    private UUID alertId;

    private String advisorId;

    private CaseStatus finalStatus;  // CLOSED_NO_ACTION or CLOSED_ACTION_TAKEN

    private String closedBy;         // the reviewer username

    private ZonedDateTime closedAt;
}
