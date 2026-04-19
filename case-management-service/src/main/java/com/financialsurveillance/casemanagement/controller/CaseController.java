package com.financialsurveillance.casemanagement.controller;

import com.financialsurveillance.casemanagement.dto.AssignRequest;
import com.financialsurveillance.casemanagement.dto.CaseDetailResponse;
import com.financialsurveillance.casemanagement.dto.CaseSummaryResponse;
import com.financialsurveillance.casemanagement.dto.TransitionRequest;
import com.financialsurveillance.casemanagement.service.CaseService;
import com.financialsurveillance.events.CaseStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/cases")
public class CaseController {

    private final CaseService caseService;

    @GetMapping
    public ResponseEntity<Page<CaseSummaryResponse>> getAllCases(@RequestParam(required = false) CaseStatus status,
                                                                 @RequestParam(required = false) String assignedTo,
                                                                 @RequestParam(required = false) String advisorId,
                                                                 @PageableDefault(size = 10) Pageable pageable){
        Page<CaseSummaryResponse> response = caseService.getAllCases(status, assignedTo, advisorId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseDetailResponse> getCaseDetail(@PathVariable("id") UUID caseId){
        CaseDetailResponse response = caseService.getCaseById(caseId);
        return  ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<CaseDetailResponse> changeStatus(@PathVariable("id") UUID caseId, @Valid @RequestBody TransitionRequest request){
        CaseDetailResponse response = caseService.transitionStatus(caseId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<CaseDetailResponse> assignCase(@PathVariable("id") UUID caseId, @Valid @RequestBody AssignRequest request){
        CaseDetailResponse response = caseService.assign(caseId, request);
        return ResponseEntity.ok(response);
    }

}
