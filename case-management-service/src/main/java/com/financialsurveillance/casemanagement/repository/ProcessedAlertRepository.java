package com.financialsurveillance.casemanagement.repository;

import com.financialsurveillance.casemanagement.domain.ProcessedAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedAlertRepository extends JpaRepository<ProcessedAlert, UUID> {
}
