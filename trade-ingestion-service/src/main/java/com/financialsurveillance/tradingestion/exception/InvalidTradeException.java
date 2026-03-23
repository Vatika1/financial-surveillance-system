package com.financialsurveillance.tradingestion.exception;

public class InvalidTradeException extends RuntimeException {

    public InvalidTradeException(String tradeId, String message) {

        super("Invalid trade [" + tradeId + "]: " + message);
    }
}
