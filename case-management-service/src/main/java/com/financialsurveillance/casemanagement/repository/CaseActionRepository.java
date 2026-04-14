package com.financialsurveillance.casemanagement.repository;

import com.financialsurveillance.casemanagement.domain.CaseAction;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

/* extending Repository instead of JpaRespository to restrict using delete(). Repository will only allow these 2 methods.
We don't want users of this repository to be able to delete() */
public interface CaseActionRepository extends Repository<CaseAction, UUID> {

    CaseAction save(CaseAction action);

    List<CaseAction> findByCaseIdOrderByPerformedAtAsc(UUID caseId);
}