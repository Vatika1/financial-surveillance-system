package com.financialsurveillance.activitymonitor.domain;

import com.fasterxml.jackson.databind.JsonNode;
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
@Table(name = "rule_violations", schema = "activity_monitor")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trade_id", nullable = false, length = 50)
    private String tradeId;

    @Column(name = "advisor_id", nullable = false, length = 50)
    private String advisorId;

    @Column(name = "rule_id", nullable = false, length = 50)
    private String ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "violation_details", columnDefinition = "jsonb")
    private JsonNode violationDetails;

    @Column(name = "detected_at", nullable = false)
    private ZonedDateTime detectedAt;


}
