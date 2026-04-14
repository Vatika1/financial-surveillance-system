package com.financialsurveillance.casemanagement.repository;

import com.financialsurveillance.casemanagement.domain.Case;
import com.financialsurveillance.events.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseRepository extends JpaRepository<Case, UUID> {

    Page<Case> findByStatus(CaseStatus status, Pageable pageable);

    Page<Case> findByAssignedTo(String assignedTo, Pageable pageable);

    Page<Case> findByAdvisorId(String advisorId, Pageable pageable);
}