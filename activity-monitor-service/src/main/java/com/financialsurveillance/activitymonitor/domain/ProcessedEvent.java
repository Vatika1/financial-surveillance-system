package com.financialsurveillance.activitymonitor.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "processed_events", schema = "activity_monitor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    @Id
    @Column(name = "trade_id", nullable = false, length = 50)
    private String tradeId;

    @Column(name = "processed_at", nullable = false)
    private ZonedDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (processedAt == null) {
            processedAt = ZonedDateTime.now();
        }
    }
}
