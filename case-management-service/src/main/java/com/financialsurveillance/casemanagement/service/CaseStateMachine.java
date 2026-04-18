package com.financialsurveillance.casemanagement.service;

import com.financialsurveillance.events.CaseStatus;

public class CaseStateMachine {

    public static boolean canTransition(CaseStatus from, CaseStatus to) {
        return switch (from) {
            case OPEN -> to == CaseStatus.IN_REVIEW;
            case IN_REVIEW -> to == CaseStatus.CLOSED_NO_ACTION
                    || to == CaseStatus.CLOSED_ACTION_TAKEN;
            case CLOSED_NO_ACTION, CLOSED_ACTION_TAKEN -> false;
        };
    }
}