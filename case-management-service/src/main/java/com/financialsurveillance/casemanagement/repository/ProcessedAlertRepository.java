package com.financialsurveillance.casemanagement.repository;

import com.financialsurveillance.casemanagement.domain.ProcessedAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedAlertRepository extends JpaRepository<ProcessedAlert, String> {
}
