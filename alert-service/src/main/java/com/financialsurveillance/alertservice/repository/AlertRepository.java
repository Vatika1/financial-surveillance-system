package com.financialsurveillance.alertservice.repository;

import com.financialsurveillance.alertservice.domain.Alert;
import com.financialsurveillance.events.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByRuleId(String ruleId);

    Optional<Alert> findBySeverity(AlertSeverity severity);

    List<Alert> findByAdvisorId(String advisorId);


    boolean existsByAlertId(UUID alertId);
}
