package com.financialsurveillance.casemanagement.domain;

import com.financialsurveillance.events.CaseStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseStateMachineTest {

    @Test
    void openCanTransitionToInReview() {
        assertTrue(CaseStateMachine.canTransition(CaseStatus.OPEN, CaseStatus.IN_REVIEW));
    }

    @Test
    void openCannotTransitionToClosedNoAction() {
        assertFalse(CaseStateMachine.canTransition(CaseStatus.OPEN, CaseStatus.CLOSED_NO_ACTION));
    }
    @Test
    void inReviewCanTransitionToClosedNoAction() {
        assertTrue(CaseStateMachine.canTransition(CaseStatus.IN_REVIEW, CaseStatus.CLOSED_NO_ACTION));
    }

    @Test
    void inReviewCanTransitionToClosedActionTaken() {
        assertTrue(CaseStateMachine.canTransition(CaseStatus.IN_REVIEW, CaseStatus.CLOSED_ACTION_TAKEN));
    }

    @Test
    void closedCannotTransitionAnywhere() {
        assertFalse(CaseStateMachine.canTransition(CaseStatus.CLOSED_NO_ACTION, CaseStatus.IN_REVIEW));
        assertFalse(CaseStateMachine.canTransition(CaseStatus.CLOSED_ACTION_TAKEN, CaseStatus.IN_REVIEW));
    }

    @Test
    void inReviewCannotTransitionBackToOpen() {
        assertFalse(CaseStateMachine.canTransition(CaseStatus.IN_REVIEW, CaseStatus.OPEN));
    }
}