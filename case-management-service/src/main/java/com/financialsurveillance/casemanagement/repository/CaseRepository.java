package com.financialsurveillance.casemanagement.repository;

import com.financialsurveillance.casemanagement.domain.Case;
import com.financialsurveillance.events.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CaseRepository extends JpaRepository<Case, UUID> {

    Page<Case> findByStatus(CaseStatus status, Pageable pageable);

    Page<Case> findByAssignedTo(String assignedTo, Pageable pageable);

    Page<Case> findByAdvisorId(String advisorId, Pageable pageable);

    @Query("SELECT c FROM Case c WHERE (:status IS NULL OR c.status = :status) " +
            "AND (:assignedTo IS NULL OR c.assignedTo = :assignedTo) " +
            "AND (:advisorId IS NULL OR c.advisorId = :advisorId)")
    Page<Case> findByFilters(@Param("status") CaseStatus status,
                             @Param("assignedTo") String assignedTo,
                             @Param("advisorId") String advisorId,
                             Pageable pageable);

    boolean existsByAlertId(UUID alertId);
}