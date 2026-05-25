package com.financialsurveillance.alertservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_alerts", schema = "alert_management")
@Getter
@Setter
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
