package com.financialsurveillance.eodreconciliation.service;

import com.financialsurveillance.eodreconciliation.dto.AdvisorDailyTotal;
import com.financialsurveillance.eodreconciliation.dto.BookingTotal;
import com.financialsurveillance.eodreconciliation.repository.ReconciliationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
public class ReconciliationProcessor implements ItemProcessor<BookingTotal, String> {

    private static final BigDecimal VALUE_TOLERANCE = new BigDecimal("0.01");

    private final ReconciliationRepository repository;
    private final LocalDate tradeDate;

    public ReconciliationProcessor(ReconciliationRepository repository, LocalDate tradeDate) {
        this.repository = repository;
        this.tradeDate = tradeDate;
    }

    @Override
    public String process(BookingTotal booked) {
        Optional<AdvisorDailyTotal> found = repository.findForUpdate(booked.advisorId(), tradeDate);

        if (found.isEmpty()) {
            log.warn("Reconciliation break advisorId={} tradeDate={} reason=missing_in_surveillance booked={}/{}",
                    booked.advisorId(), tradeDate, booked.tradeCount(), booked.totalValue());
            return null;
        }

        AdvisorDailyTotal seen = found.get();
        boolean countMatches = seen.tradeCount() == booked.tradeCount();
        boolean valueMatches = seen.totalValue().subtract(booked.totalValue()).abs()
                .compareTo(VALUE_TOLERANCE) <= 0;

        if (countMatches && valueMatches) {
            return booked.advisorId();
        }

        log.warn("Reconciliation break advisorId={} tradeDate={} reason=totals_mismatch surveillance={}/{} booked={}/{}",
                booked.advisorId(), tradeDate, seen.tradeCount(), seen.totalValue(),
                booked.tradeCount(), booked.totalValue());
        return null;
    }
}