package com.financialsurveillance.tradingestion.producer;

import com.financialsurveillance.tradingestion.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {
    public OutboxMetrics(MeterRegistry registry, OutboxEventRepository outboxRepository) {
        Gauge.builder("outbox_unsent_rows", outboxRepository, OutboxEventRepository::countBySentAtIsNull)
                .description("Outbox rows waiting to be published")
                .register(registry);
    }
}
