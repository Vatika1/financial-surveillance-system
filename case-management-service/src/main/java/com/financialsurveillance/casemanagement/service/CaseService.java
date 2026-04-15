package com.financialsurveillance.casemanagement.service;

import com.financialsurveillance.casemanagement.domain.ActionType;
import com.financialsurveillance.casemanagement.domain.Case;
import com.financialsurveillance.casemanagement.domain.CaseAction;
import com.financialsurveillance.casemanagement.dto.CaseDetailResponse;
import com.financialsurveillance.casemanagement.mapper.CaseMapper;
import com.financialsurveillance.casemanagement.producer.CaseEventProducer;
import com.financialsurveillance.casemanagement.repository.CaseActionRepository;
import com.financialsurveillance.casemanagement.repository.CaseRepository;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.CaseCreatedEvent;
import com.financialsurveillance.events.CaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseEventProducer producer;
    private final CaseActionRepository caseActionRepository;
    private final CaseMapper caseMapper;

    @Transactional
    public void createCaseFromAlert(AlertPersistedEvent event){
        log.info("Creating case for Alert alertId={} alertTypeId={} tradeId={} advisorId={}",
                event.getAlertId(), event.getAlertTypeId(), event.getTradeId(), event.getAdvisorId());

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

    @Transactional
    public CaseDetailResponse transitionStatus(UUID caseId, CaseStatus newStatus, String performedBy){

        return null;
    }

    @Transactional
    public CaseDetailResponse  assign(UUID caseId, String assignedTo, String performedBy){

        return null;
    }

}
