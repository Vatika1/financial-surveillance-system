package com.financialsurveillance.activitymonitor.repository;

import com.financialsurveillance.activitymonitor.domain.RuleViolation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RuleViolationRepository extends JpaRepository<RuleViolation, UUID> {

    //only need save() method for now

}
