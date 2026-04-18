package com.financialsurveillance.casemanagement.service;

import com.financialsurveillance.casemanagement.domain.ActionType;
import com.financialsurveillance.casemanagement.domain.Case;
import com.financialsurveillance.casemanagement.domain.CaseAction;
import com.financialsurveillance.casemanagement.dto.AssignRequest;
import com.financialsurveillance.casemanagement.dto.CaseActionResponse;
import com.financialsurveillance.casemanagement.dto.CaseDetailResponse;
import com.financialsurveillance.casemanagement.dto.TransitionRequest;
import com.financialsurveillance.casemanagement.exception.CaseNotFoundException;
import com.financialsurveillance.casemanagement.exception.IllegalStateTransitionException;
import com.financialsurveillance.casemanagement.mapper.CaseMapper;
import com.financialsurveillance.casemanagement.producer.CaseEventProducer;
import com.financialsurveillance.casemanagement.repository.CaseActionRepository;
import com.financialsurveillance.casemanagement.repository.CaseRepository;
import com.financialsurveillance.events.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class CaseServiceTest {

    @Mock
    private CaseMapper caseMapper;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseActionRepository caseActionRepository;
    @Mock
    private CaseEventProducer producer;
    @InjectMocks
    private CaseService caseService;

    private AlertPersistedEvent getAlertPersistedEvent(){
        return AlertPersistedEvent.builder()
                .alertId(UUID.randomUUID())
                .alertTypeId("AHS-20260415100000")
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .ruleId("RULE_001")
                .ruleName("High-value transaction threshold")
                .createdAt(ZonedDateTime.now())
                .persistedAt(ZonedDateTime.now())
                .violationDetails(null)
                .severity(AlertSeverity.HIGH)
                .status(AlertStatus.OPEN)
                .build();
    }

    private TransitionRequest getTransitionRequest(CaseStatus status){
        return TransitionRequest.builder()
                .newStatus(status)
                .performedBy("sarah.chen")
                .build();
    }

    private AssignRequest getAssignRequest(String assignedTo){
        return AssignRequest.builder()
                .assignedTo(assignedTo)
                .performedBy("sarah.chen")
                .build();
    }

    private Case buildCase(UUID caseId,  CaseStatus status){
        return Case.builder()
                .id(caseId)
                .status(status)
                .alertId(UUID.randomUUID())
                .advisorId("ADV-001")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    private CaseAction getCaseAction(UUID caseId, String fromValue, String toValue, ActionType actionType){
        return CaseAction.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .actionType(actionType)
                .performedBy("sarah.chen")
                .performedAt(ZonedDateTime.now())
                .fromValue(fromValue)
                .toValue(toValue)
                .build();
    }

    private CaseDetailResponse getCaseDetailResponse(UUID caseId, CaseStatus status){
        return CaseDetailResponse.builder()
                .id(caseId)
                .status(status)
                .build();
    }

    private CaseActionResponse getCaseActionResponse(UUID caseId, CaseAction action,  ActionType actionType){
        return CaseActionResponse.builder()
                .id(action.getId())
                .actionType(actionType)
                .build();
    }

    @Test
    void createCaseFromAlert_shouldCreateCaseWhenAlertEventReceived(){
        AlertPersistedEvent event = getAlertPersistedEvent();

        when(caseRepository.existsByAlertId(event.getAlertId())).thenReturn(false);
        when(caseRepository.save(any(Case.class))).thenAnswer(i -> i.getArgument(0));

        caseService.createCaseFromAlert(event);

        verify(caseRepository).save(any(Case.class));
        verify(caseActionRepository).save(any(CaseAction.class));
        verify(producer).publishCaseCreated(any(CaseCreatedEvent.class));
    }

    @Test
    void createCaseFromAlert_ShouldFailCaseAlreadyExists(){
        AlertPersistedEvent event = getAlertPersistedEvent();
        when(caseRepository.existsByAlertId(event.getAlertId())).thenReturn(true);

        caseService.createCaseFromAlert(event);

        verify(caseRepository, never()).save(any());
        verify(producer, never()).publishCaseCreated(any(CaseCreatedEvent.class));

    }

    @Test
    void transitionStatus_ShouldSuccessfullyTransitionStatus(){
        TransitionRequest request = getTransitionRequest(CaseStatus.IN_REVIEW);
        UUID caseId = UUID.randomUUID();

        Case existingCase = buildCase(caseId, CaseStatus.OPEN);
        CaseAction action = getCaseAction(caseId,"OPEN", "IN_REVIEW", ActionType.STATUS_CHANGED);
        CaseDetailResponse detailResponse = getCaseDetailResponse(caseId, CaseStatus.OPEN);
        CaseActionResponse actionResponse = getCaseActionResponse(caseId, action, ActionType.STATUS_CHANGED);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(caseRepository.save(any(Case.class))).thenAnswer(i -> i.getArgument(0));
        when(caseMapper.toCaseDetailResponse(any())).thenReturn(detailResponse);
        when(caseActionRepository.findByCaseIdOrderByPerformedAtAsc(caseId)).thenReturn(List.of(action));
        when(caseMapper.toCaseActionResponse(any(CaseAction.class))).thenReturn(actionResponse);

        CaseDetailResponse response = caseService.transitionStatus(caseId, request);
        assertNotNull(response);

        verify(caseRepository).save(any(Case.class));
        verify(caseActionRepository).save(any(CaseAction.class));
        verify(producer, never()).publishCaseClosed(any());
    }

    @Test
    void transitionStatus_ShouldSuccessfullyTransitionClosedStatus_triggerCaseClosedEvent(){
        TransitionRequest request = getTransitionRequest(CaseStatus.CLOSED_ACTION_TAKEN);
        UUID caseId = UUID.randomUUID();

        Case existingCase = buildCase(caseId, CaseStatus.IN_REVIEW);
        CaseAction action = getCaseAction(caseId, "IN_REVIEW", "CLOSED_ACTION_TAKEN", ActionType.STATUS_CHANGED);
        CaseDetailResponse detailResponse = getCaseDetailResponse(caseId, CaseStatus.CLOSED_ACTION_TAKEN);
        CaseActionResponse actionResponse = getCaseActionResponse(caseId, action,  ActionType.STATUS_CHANGED);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(caseRepository.save(any(Case.class))).thenAnswer(i -> i.getArgument(0));
        when(caseActionRepository.save(any(CaseAction.class))).thenAnswer(i -> i.getArgument(0));
        when(caseMapper.toCaseDetailResponse(any())).thenReturn(detailResponse);
        when(caseActionRepository.findByCaseIdOrderByPerformedAtAsc(caseId)).thenReturn(List.of(action));
        when(caseMapper.toCaseActionResponse(any(CaseAction.class))).thenReturn(actionResponse);

        CaseDetailResponse response = caseService.transitionStatus(caseId, request);

        assertNotNull(response);

        verify(caseRepository).save(any(Case.class));
        verify(caseActionRepository).save(any(CaseAction.class));
        verify(producer).publishCaseClosed(any(CaseClosedEvent.class));
    }

    @Test
    void transitionStatus_ShouldFail_whenCaseNotFound(){
        TransitionRequest request = getTransitionRequest(CaseStatus.IN_REVIEW);

        UUID caseId = UUID.randomUUID();

        when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

        assertThrows(CaseNotFoundException.class, () -> {
            caseService.transitionStatus(caseId, request);
        });

        verify(caseRepository).findById(caseId);
        verify(caseRepository, never()).save(any());
        verify(caseActionRepository, never()).save(any());
        verify(producer, never()).publishCaseClosed(any());
    }

    @Test
    void transitionStatus_ShouldFail_whenIllegalTransition(){
        TransitionRequest request = getTransitionRequest(CaseStatus.CLOSED_NO_ACTION);

        UUID caseId = UUID.randomUUID();

        Case existingCase = buildCase(caseId, CaseStatus.OPEN);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));

        assertThrows(IllegalStateTransitionException.class, () -> {
            caseService.transitionStatus(caseId, request);
        });
    }

    @Test
    void assign_ShouldSuccessfullyAssignAReviewer(){
        AssignRequest request = getAssignRequest("sarah.chen");

        UUID caseId = UUID.randomUUID();
        Case existingCase = buildCase(caseId, CaseStatus.IN_REVIEW);
        CaseAction action = getCaseAction(caseId, "null", "sarah.chen", ActionType.ASSIGNED);
        CaseDetailResponse detailResponse = getCaseDetailResponse(caseId, CaseStatus.IN_REVIEW);
        CaseActionResponse actionResponse = getCaseActionResponse(caseId, action, ActionType.ASSIGNED);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(caseRepository.save(any(Case.class))).thenAnswer(i -> i.getArgument(0));
        when(caseActionRepository.save(any(CaseAction.class))).thenAnswer(i -> i.getArgument(0));
        when(caseMapper.toCaseDetailResponse(any())).thenReturn(detailResponse);
        when(caseActionRepository.findByCaseIdOrderByPerformedAtAsc(caseId)).thenReturn(List.of(action));
        when(caseMapper.toCaseActionResponse(any(CaseAction.class))).thenReturn(actionResponse);

        CaseDetailResponse response = caseService.assign(caseId, request);
        assertNotNull(response);

        verify(caseRepository).save(any(Case.class));
        verify(caseActionRepository).save(any(CaseAction.class));

    }

    @Test
    void assign_ShouldSuccessfullyUnAssignAReviewer(){
        AssignRequest request = getAssignRequest("null");

        UUID caseId = UUID.randomUUID();
        Case existingCase = buildCase(caseId, CaseStatus.IN_REVIEW);

        CaseAction action = getCaseAction(caseId, "sarah.chen", "null", ActionType.UNASSIGNED);
        CaseDetailResponse detailResponse = getCaseDetailResponse(caseId, CaseStatus.IN_REVIEW);
        CaseActionResponse actionResponse = getCaseActionResponse(caseId, action, ActionType.UNASSIGNED);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(caseRepository.save(any(Case.class))).thenAnswer(i -> i.getArgument(0));
        when(caseActionRepository.save(any(CaseAction.class))).thenAnswer(i -> i.getArgument(0));
        when(caseMapper.toCaseDetailResponse(any())).thenReturn(detailResponse);
        when(caseActionRepository.findByCaseIdOrderByPerformedAtAsc(caseId)).thenReturn(List.of(action));
        when(caseMapper.toCaseActionResponse(any(CaseAction.class))).thenReturn(actionResponse);

        CaseDetailResponse response = caseService.assign(caseId, request);
        assertNotNull(response);

        verify(caseRepository).findById(caseId);
        verify(caseRepository).save(any(Case.class));
        verify(caseActionRepository).save(any(CaseAction.class));


    }

    @Test
    void assign_ShouldFail_CaseNotFound(){
        AssignRequest request = getAssignRequest("sarah.chen");
        UUID caseId = UUID.randomUUID();

        when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

        assertThrows(CaseNotFoundException.class, () -> {
            caseService.assign(caseId, request);
        });

        verify(caseRepository).findById(caseId);
        verify(caseRepository, never()).save(any());
        verify(caseActionRepository, never()).save(any());
        verify(producer, never()).publishCaseClosed(any());
    }

    @Test
    void assign_ShouldFail_whenIllegalTransitionForClosedCase(){
        AssignRequest request = getAssignRequest("sarah.chen");
        UUID caseId = UUID.randomUUID();
        Case existingCase = buildCase(caseId, CaseStatus.CLOSED_NO_ACTION);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));

        assertThrows(IllegalStateTransitionException.class, () -> {
            caseService.assign(caseId, request);
        });

        verify(caseRepository).findById(caseId);
    }


}
