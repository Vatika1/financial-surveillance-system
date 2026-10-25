package com.financialsurveillance.eodreconciliation.dto;

import java.math.BigDecimal;

public record AdvisorDailyTotal(String advisorId, int tradeCount, BigDecimal totalValue) {
}