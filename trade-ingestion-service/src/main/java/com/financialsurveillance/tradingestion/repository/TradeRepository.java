package com.financialsurveillance.tradingestion.repository;

import com.financialsurveillance.tradingestion.domain.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradeRepository extends JpaRepository<Trade, UUID> {

    Optional<Trade> findByTradeId(String tradeId);

    List<Trade> findByAdvisorId(String advisorId);

    List<Trade> findByAdvisorIdAndSymbol(String advisorId, String symbol);

    List<Trade> findByAdvisorIdAndTradeTimestampBetween(
            String advisorId,
            ZonedDateTime from,
            ZonedDateTime to
    );

    boolean existsByTradeId(String tradeId);
}