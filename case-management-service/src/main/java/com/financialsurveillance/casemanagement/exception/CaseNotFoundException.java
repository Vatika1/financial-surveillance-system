package com.financialsurveillance.casemanagement.exception;

import java.util.UUID;

public class CaseNotFoundException extends RuntimeException{

    public CaseNotFoundException(UUID caseId){
        super("Case not found for id: " + caseId);
    }
}
