package com.financialsurveillance.alertservice.exception;

import java.util.UUID;

public class AlertPersistedPublishException extends RuntimeException{
    public AlertPersistedPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
