package com.financialsurveillance.tradingestion.domain;

import com.financialsurveillance.events.TradeStatus;
import com.financialsurveillance.events.TradeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "trades", schema = "trade_ingestion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trade_id", unique = true, nullable = false, length = 50)
    private String tradeId;

    @Column(name = "advisor_id", nullable = false, length = 50)
    private String advisorId;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "client_id", nullable = false, length = 50)
    private String clientId;

    @Column(name = "symbol", nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 10)
    private TradeType tradeType;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity;

    @Column(name = "price", nullable = false, precision = 15, scale = 4)
    private BigDecimal price;

    @Column(name = "total_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "exchange", length = 20)
    private String exchange;

    @Column(name = "trade_timestamp", nullable = false)
    private ZonedDateTime tradeTimestamp;

    @Column(name = "source_system", length = 50)
    private String sourceSystem;

    @Column(name = "source_system_id", length = 100)
    private String sourceSystemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TradeStatus status;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (currency == null) {
            currency = "USD";
        }
        if (status == null) {
            status = TradeStatus.RECEIVED;
        }
    }
}