package com.financialsurveillance.casemanagement.service;

import com.financialsurveillance.casemanagement.producer.CaseEventProducer;
import com.financialsurveillance.casemanagement.repository.CaseActionRepository;
import com.financialsurveillance.casemanagement.repository.CaseRepository;
import com.financialsurveillance.events.AlertCreatedEvent;
import com.financialsurveillance.events.AlertPersistedEvent;
import com.financialsurveillance.events.CaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseEventProducer producer;
    private final CaseActionRepository caseActionRepository;

    @Transactional
    public void createCaseFromAlert(AlertPersistedEvent event){

    }

    @Transactional
    public void transitionStatus(UUID caseId, CaseStatus newStatus, String performedBy){

    }

    @Transactional
    public void assign(UUID caseId, String assignedTo, String performedBy){

    }

}
