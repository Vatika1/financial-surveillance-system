package com.financialsurveillance.casemanagement.mapper;

import com.financialsurveillance.casemanagement.domain.Case;
import com.financialsurveillance.casemanagement.domain.CaseAction;
import com.financialsurveillance.casemanagement.dto.CaseActionResponse;
import com.financialsurveillance.casemanagement.dto.CaseDetailResponse;
import com.financialsurveillance.casemanagement.dto.CaseSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CaseMapper {
    CaseDetailResponse toCaseDetailResponse(Case c);
    CaseSummaryResponse toCaseSummaryResponse(Case c);
    CaseActionResponse toCaseActionResponse(CaseAction caseAction);
}
