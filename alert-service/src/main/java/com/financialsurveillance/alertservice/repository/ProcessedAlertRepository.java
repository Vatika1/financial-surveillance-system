package com.financialsurveillance.alertservice.repository;

import com.financialsurveillance.alertservice.domain.ProcessedAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedAlertRepository  extends JpaRepository<ProcessedAlert, UUID> {
}
