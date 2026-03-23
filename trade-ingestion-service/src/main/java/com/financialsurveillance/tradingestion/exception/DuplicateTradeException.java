package com.financialsurveillance.tradingestion.exception;

public class DuplicateTradeException extends RuntimeException{

    public DuplicateTradeException(String tradeId) {
        super("Trade already exists with ID: "+ tradeId);
    }
}
