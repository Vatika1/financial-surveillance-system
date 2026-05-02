package com.financialsurveillance.activitymonitor.repository;

import com.financialsurveillance.activitymonitor.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
}
