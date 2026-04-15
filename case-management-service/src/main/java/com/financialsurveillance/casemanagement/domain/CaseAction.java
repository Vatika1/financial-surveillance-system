package com.financialsurveillance.casemanagement.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "case_actions", schema = "case_management")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseAction {

    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private ActionType actionType;

    @Column(name = "performed_by", nullable = false, length = 64)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private ZonedDateTime performedAt;

    @Column(name = "from_value", length = 128)
    private String fromValue;

    @Column(name = "to_value", length = 128)
    private String toValue;
}