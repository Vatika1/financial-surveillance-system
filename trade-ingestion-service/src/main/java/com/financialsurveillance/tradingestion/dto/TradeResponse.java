package com.financialsurveillance.tradingestion.dto;

import com.financialsurveillance.events.TradeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    private UUID id;
    private String tradeId;
    private ZonedDateTime createdAt;
    private TradeStatus status;
}
