package com.financialsurveillance.casemanagement.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_alerts", schema = "case_management")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedAlert {

    @Id
    @Column(name = "alert_id", nullable = false, length = 50)
    private UUID alertId;

    @Column(name = "processed_at", nullable = false)
    private ZonedDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (processedAt == null) {
            processedAt = ZonedDateTime.now();
        }

    }
}
