package com.financialsurveillance.casemanagement.dto;

import com.financialsurveillance.events.CaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionRequest {

    @NotNull(message = "CaseStatus is required")
    private CaseStatus newStatus;

    @NotBlank(message = "performedBy is required")
    private String performedBy;

}
