package com.financialsurveillance.tradingestion.dto;

import com.financialsurveillance.events.TradeType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequest {

    @NotBlank (message = "TradeId id is required")
    private String tradeId;

    @NotBlank (message = "AdvisorId id is required")
    private String advisorId;

    @NotBlank (message = "AccountId id is required")
    private String accountId;

    @NotBlank (message = "ClientId id is required")
    private String clientId;

    @Pattern(regexp = "^[A-Z]{1,5}$", message = "Symbol must be 1-5 uppercase letters")
    @NotBlank (message = "Symbol id is required")
    private String symbol;

    @NotNull(message = "TradeType id is required")
    private TradeType tradeType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private String currency;

    private String exchange;

    @NotNull(message = "Trade timestamp is required")
    @PastOrPresent(message = "Trade timestamp cannot be in the future")
    private ZonedDateTime tradeTimestamp;

    @NotBlank(message = "SourceSystem ID is required")
    private String sourceSystem;

    @NotBlank(message = "SourceSystemId ID is required")
    private String sourceSystemId;
}
