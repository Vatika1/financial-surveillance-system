package com.financialsurveillance.casemanagement.mapper;

import com.financialsurveillance.casemanagement.domain.Case;
import com.financialsurveillance.casemanagement.dto.CaseResponse;
import com.financialsurveillance.casemanagement.dto.CaseSummaryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CaseMapper {
    CaseResponse toCaseResponse(Case c);
    CaseSummaryResponse toCaseSummaryResponse(Case c);
}
