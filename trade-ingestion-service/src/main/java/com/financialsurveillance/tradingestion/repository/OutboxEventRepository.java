package com.financialsurveillance.tradingestion.repository;

import com.financialsurveillance.tradingestion.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = "SELECT * FROM trade_ingestion.outbox WHERE sent_at IS NULL ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findUnsentBatch();

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM trade_ingestion.outbox WHERE sent_at < :cutoff", nativeQuery = true)
    int deleteSentBefore(@Param("cutoff") ZonedDateTime cutoff);
}
