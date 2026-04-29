package com.financialsurveillance.tradingestion.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicsProperties(
        @NotBlank String tradesRaw,
        @NotBlank String tradesRawDlt
) {}