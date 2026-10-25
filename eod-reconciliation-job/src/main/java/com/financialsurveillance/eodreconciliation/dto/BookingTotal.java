package com.financialsurveillance.eodreconciliation.dto;

import java.math.BigDecimal;

public record BookingTotal(String advisorId, int tradeCount, BigDecimal totalValue) {
}