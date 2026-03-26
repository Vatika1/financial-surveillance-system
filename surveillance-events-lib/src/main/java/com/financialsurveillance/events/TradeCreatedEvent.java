package com.financialsurveillance.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeCreatedEvent {

    private UUID id;
    private String tradeId;
    private String advisorId;
    private String accountId;
    private String clientId;
    private String symbol;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalValue;
    private String currency;
    private String exchange;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime tradeTimestamp;

    private String sourceSystem;
    private String sourceSystemId;
    private TradeStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private ZonedDateTime createdAt;

}
