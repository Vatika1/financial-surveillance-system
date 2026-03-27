package com.financialsurveillance.alertservice.exception;

import java.util.UUID;

public class AlertProcessingException extends RuntimeException{

    public AlertProcessingException(UUID alertId, String alertTypeId, String advisorId, Throwable cause){
        super("Failed to process alert for alertId: " + alertId + " alertTypeId: " + alertTypeId + " advisorId: " + advisorId, cause);
    }
}
