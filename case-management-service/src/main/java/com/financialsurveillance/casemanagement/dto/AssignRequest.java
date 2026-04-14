package com.financialsurveillance.casemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRequest {

    private String assignedTo;

    @NotBlank(message = "performedBy is required")
    private String performedBy;
}
