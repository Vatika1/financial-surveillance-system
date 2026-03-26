package com.financialsurveillance.activitymonitor.exception;

public class TradeProcessingException extends RuntimeException{

    public TradeProcessingException(String tradeId,String advisorId, Throwable cause){
        super("Failed to process trade for tradeId: " + tradeId + " advisorId: " + advisorId, cause);
    }


}
