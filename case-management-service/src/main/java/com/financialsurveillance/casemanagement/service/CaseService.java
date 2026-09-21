package com.financialsurveillance.casemanagement.service;

import com.financialsurveillance.casemanagement.domain.ActionType;
import com.financialsurveillance.casemanagement.domain.Case;
import com.financialsurveillance.casemanagement.domain.CaseAction;
import com.financialsurveillance.casemanagement.dto.*;
import com.financialsurveillance.casemanagement.exception.CaseNotFoundException;
import com.financialsurveillance.casemanagement.exception.IllegalStateTransitionException;
import com.financialsurveillance.casemanagement.mapper.CaseMapper;
import com.financialsurveillance.casemanagement.producer.CaseEventProducer;
import com.financialsurveillance.casemanagement.repository.CaseActionRepository;
import com.financialsurveillance.casemanagement.repository.CaseRepository;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.CaseClosedEvent;
import com.financialsurveillance.events.CaseCreatedEvent;
import com.financialsurveillance.events.CaseStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseEventProducer producer;
    private final CaseActionRepository caseActionRepository;
    private final CaseMapper caseMapper;
    private final EntityManager entityManager;

    public CaseDetailResponse getCaseById(UUID caseId){
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));

        return caseMapper.toCaseDetailResponse(caseEntity);
    }
    public Page<CaseSummaryResponse> getAllCases(CaseStatus status,
                                                 String assignedTo,
                                                 String advisorId,Pageable pageable){

        Page<Case> cases = caseRepository.findByFilters(status, assignedTo, advisorId, pageable);

        return cases.map(caseMapper::toCaseSummaryResponse);
    }
    @Transactional
    public void createCaseFromAlert(AlertPersistedEvent event){
        log.info("Creating case for Alert alertId={} alertTypeId={} tradeId={} advisorId={}",
                event.getAlertId(), event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

        if (caseRepository.existsByAlertId(event.getAlertId())) {
            log.warn("Case already exists for alertId={}", event.getAlertId());
            return;
        }

        Case newCase = Case.builder()
                .id(UUID.randomUUID())
                .alertId(event.getAlertId())
                .createdAt(event.getCreatedAt())
                .advisorId(event.getAdvisorId())
                .status(CaseStatus.OPEN)
                .updatedAt(ZonedDateTime.now())
                .build();

        Case savedCase = caseRepository.save(newCase);

        CaseAction caseAction = CaseAction.builder()
                .id(UUID.randomUUID())
                .actionType(ActionType.CASE_CREATED)
                .caseId(savedCase.getId())
                .performedAt(ZonedDateTime.now())
                .performedBy("system")
                .build();

        caseActionRepository.save(caseAction);

        CaseCreatedEvent caseEvent = CaseCreatedEvent.builder()
                .createdAt(savedCase.getCreatedAt())
                .caseId(savedCase.getId())
                .alertId(savedCase.getAlertId())
                .advisorId(savedCase.getAdvisorId())
                .status(savedCase.getStatus())
                .build();

        producer.publishCaseCreated(caseEvent);
    }

    /**
     * Batch version of createCaseFromAlert. Caller has already removed duplicates.
     * Uses persist() rather than save() so no SELECT-before-INSERT per row;
     * Hibernate flushes cases and case_actions as batched INSERTs at commit.
     */
    @Transactional
    public void createCasesFromAlerts(List<AlertPersistedEvent> events) {
        log.info("Creating {} cases from alert batch", events.size());

        ZonedDateTime now = ZonedDateTime.now();
        List<CaseCreatedEvent> caseEvents = new ArrayList<>(events.size());

        for (AlertPersistedEvent event : events) {
            log.info("Creating case for Alert {}", event.getAlertId());
            Case newCase = Case.builder()
                    .id(UUID.randomUUID())
                    .alertId(event.getAlertId())
                    .createdAt(event.getCreatedAt())
                    .advisorId(event.getAdvisorId())
                    .status(CaseStatus.OPEN)
                    .updatedAt(now)
                    .build();
            entityManager.persist(newCase);

            CaseAction caseAction = CaseAction.builder()
                    .id(UUID.randomUUID())
                    .actionType(ActionType.CASE_CREATED)
                    .caseId(newCase.getId())
                    .performedAt(now)
                    .performedBy("system")
                    .build();
            entityManager.persist(caseAction);

            caseEvents.add(CaseCreatedEvent.builder()
                    .createdAt(newCase.getCreatedAt())
                    .caseId(newCase.getId())
                    .alertId(newCase.getAlertId())
                    .advisorId(newCase.getAdvisorId())
                    .status(newCase.getStatus())
                    .build());
        }

        for (CaseCreatedEvent caseEvent : caseEvents) {
            producer.publishCaseCreated(caseEvent);
        }
    }

    @Transactional
    public CaseDetailResponse transitionStatus(UUID caseId, TransitionRequest request){
        Case existingCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));
        if(!CaseStateMachine.canTransition(existingCase.getStatus(), request.getNewStatus())){
            log.warn("Illegal state transition from " + existingCase.getStatus() + " to " + request.getNewStatus());
            throw new IllegalStateTransitionException(caseId);
        }

        CaseStatus oldStatus = existingCase.getStatus();
        existingCase.setStatus(request.getNewStatus());
        existingCase.setUpdatedAt(ZonedDateTime.now());
        if(isClosedStatus(request.getNewStatus())){
            existingCase.setClosedAt(ZonedDateTime.now());
        }
        caseRepository.save(existingCase);

        CaseAction caseAction = CaseAction.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .actionType(ActionType.STATUS_CHANGED)
                .performedBy(request.getPerformedBy())
                .performedAt(ZonedDateTime.now())
                .fromValue(oldStatus.name())
                .toValue(existingCase.getStatus().name())
                .build();
        caseActionRepository.save(caseAction);

        if(isClosedStatus(request.getNewStatus())){
            CaseClosedEvent closedEvent = CaseClosedEvent.builder()
                    .alertId(existingCase.getAlertId())
                    .closedAt(existingCase.getClosedAt())
                    .caseId(existingCase.getId())
                    .finalStatus(existingCase.getStatus())
                    .advisorId(existingCase.getAdvisorId())
                    .closedBy(request.getPerformedBy())
                    .build();

            producer.publishCaseClosed(closedEvent);
        }

        CaseDetailResponse response = caseMapper.toCaseDetailResponse(existingCase);
        List<CaseAction> auditTrail = caseActionRepository.findByCaseIdOrderByPerformedAtAsc(caseId);
        List<CaseActionResponse> auditTrailResponse = auditTrail.stream()
                .map(caseMapper::toCaseActionResponse)
                .toList();

        response.setAuditTrails(auditTrailResponse);
        return response;
    }

    public boolean isClosedStatus(CaseStatus status){
        return status == CaseStatus.CLOSED_ACTION_TAKEN
                || status == CaseStatus.CLOSED_NO_ACTION;
    }

    @Transactional
    public CaseDetailResponse  assign(UUID caseId, AssignRequest request){
        Case existingCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));

        if(isClosedStatus(existingCase.getStatus())){
            log.warn("Illegal to assign a closed case for case: " + caseId);
            throw new IllegalStateTransitionException(caseId);
        }
        String oldAssignedTo = existingCase.getAssignedTo();
        existingCase.setAssignedTo(request.getAssignedTo());
        existingCase.setUpdatedAt(ZonedDateTime.now());

        caseRepository.save(existingCase);
        ActionType actionType;

        if(request.getAssignedTo() == null){
            actionType = ActionType.UNASSIGNED;
        }
        else {
            actionType = ActionType.ASSIGNED;
        }

        CaseAction caseAction = CaseAction.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .actionType(actionType)
                .performedBy(request.getPerformedBy())
                .performedAt(ZonedDateTime.now())
                .fromValue(oldAssignedTo)
                .toValue(existingCase.getAssignedTo())
                .build();
        caseActionRepository.save(caseAction);

        CaseDetailResponse response = caseMapper.toCaseDetailResponse(existingCase);
        List<CaseAction> auditTrail = caseActionRepository.findByCaseIdOrderByPerformedAtAsc(caseId);
        List<CaseActionResponse> auditTrailResponse = auditTrail.stream()
                .map(caseMapper::toCaseActionResponse)
                .toList();

        response.setAuditTrails(auditTrailResponse);
        return response;
    }

}