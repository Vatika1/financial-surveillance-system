package com.financialsurveillance.casemanagement.exception;

import java.util.UUID;

public class IllegalStateTransitionException extends RuntimeException{

    public IllegalStateTransitionException(UUID caseId){
        super("Illegal transition for case: " + caseId);
    }
}
