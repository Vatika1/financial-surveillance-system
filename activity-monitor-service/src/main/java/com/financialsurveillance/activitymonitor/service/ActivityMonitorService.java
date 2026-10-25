package com.financialsurveillance.activitymonitor.service;

import com.financialsurveillance.activitymonitor.cache.TradeWindowStore;
import com.financialsurveillance.activitymonitor.domain.RuleViolation;
import com.financialsurveillance.activitymonitor.dto.RuleContext;
import com.financialsurveillance.activitymonitor.dto.RuleViolationDTO;
import com.financialsurveillance.activitymonitor.engine.SurveillanceEngine;
import com.financialsurveillance.activitymonitor.mapper.RuleViolationMapper;
import com.financialsurveillance.activitymonitor.producer.AlertEventProducer;
import com.financialsurveillance.activitymonitor.repository.AdvisorDailyActivityRepository;
import com.financialsurveillance.activitymonitor.repository.RuleViolationRepository;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.TradeCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class ActivityMonitorService {

    private final RuleViolationRepository ruleViolationRepository;
    private final AdvisorDailyActivityRepository advisorDailyActivityRepository;
    private final TradeWindowStore tradeWindowStore;
    private final SurveillanceEngine surveillanceEngine;
    private final AlertEventProducer alertEventProducer;
    private final static Set<String> RESTRICTED_SYMBOLS = Set.of("AAPL", "TSLA", "MSFT", "GOOGL", "AMZN");
    private final RuleViolationMapper ruleViolationMapper;
    private static final Duration WINDOW = Duration.ofSeconds(60);
    private static final ZoneId TRADING_ZONE = ZoneId.of("America/New_York");

    @Transactional
    public void processTrades(List<TradeCreatedEvent> events){
        log.info("Processing {} trades from alert batch", events.size());

        List<AlertCreatedEvent> alerts = new ArrayList<>(events.size());
        // Key = "advisorId|tradeDate". TreeMap keeps the keys sorted.
        Map<String, Integer> countByAdvisorDay = new TreeMap<>();
        Map<String, BigDecimal> valueByAdvisorDay = new TreeMap<>();

        for(TradeCreatedEvent event: events){
            tradeWindowStore.addTrade(event.getAdvisorId(), event);
            LocalDate tradeDate = event.getTradeTimestamp() != null
                    ? event.getTradeTimestamp().withZoneSameInstant(TRADING_ZONE).toLocalDate()
                    : LocalDate.now(TRADING_ZONE);
            String advisorDay = event.getAdvisorId() + "|" + tradeDate;
            countByAdvisorDay.merge(advisorDay, 1, Integer::sum);
            valueByAdvisorDay.merge(advisorDay,
                    event.getTotalValue() == null ? BigDecimal.ZERO : event.getTotalValue(),
                    BigDecimal::add);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                tradeWindowStore.removeTrade(event.getAdvisorId(), event);
                            }
                        }
                    });
            List<TradeCreatedEvent> recentTrades =
                    tradeWindowStore.getRecentTrades(event.getAdvisorId(), WINDOW);

            RuleContext ruleContext = new RuleContext(recentTrades, RESTRICTED_SYMBOLS, Optional.empty());
            List<RuleViolationDTO> violationList = surveillanceEngine.evaluate(event, ruleContext);
            for (RuleViolationDTO violation : violationList) {
                RuleViolation violationObj = ruleViolationMapper.toEntity(violation);
                ruleViolationRepository.save(violationObj);
                alerts.add(alertEventProducer.toAlertEvent(violation, event));
            }
        }

        // One upsert per advisor per day, in sorted order: every consumer thread
        // locks rows in the same order, so they can wait but never deadlock.
        for (String advisorDay : countByAdvisorDay.keySet()) {
            String[] parts = advisorDay.split("\\|");
            advisorDailyActivityRepository.recordTrades(
                    parts[0], LocalDate.parse(parts[1]),
                    countByAdvisorDay.get(advisorDay), valueByAdvisorDay.get(advisorDay));
        }

        alertEventProducer.publishAll(alerts);
    }
}