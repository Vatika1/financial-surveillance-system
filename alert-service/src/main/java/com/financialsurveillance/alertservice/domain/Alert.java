package com.financialsurveillance.alertservice.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.financialsurveillance.events.AlertSeverity;
import com.financialsurveillance.events.AlertStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts", schema = "alert_management")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @Column(name = "alert_id", updatable = false, nullable = false)
    private UUID alertId;

    @Column(name = "alert_type_id", nullable = false, length = 50)
    private String alertTypeId;

    @Column(name = "trade_id", nullable = false, length = 50)
    private String tradeId;

    @Column(name = "advisor_id", nullable = false, length = 50)
    private String advisorId;

    @Column(name = "rule_id", nullable = false, length = 50)
    private String ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "violation_details", columnDefinition = "jsonb")
    private JsonNode violationDetails;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;


}
