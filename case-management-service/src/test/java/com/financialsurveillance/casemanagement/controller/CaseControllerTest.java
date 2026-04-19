package com.financialsurveillance.casemanagement.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.casemanagement.dto.AssignRequest;
import com.financialsurveillance.casemanagement.dto.CaseDetailResponse;
import com.financialsurveillance.casemanagement.dto.CaseSummaryResponse;
import com.financialsurveillance.casemanagement.dto.TransitionRequest;
import com.financialsurveillance.casemanagement.exception.CaseNotFoundException;
import com.financialsurveillance.casemanagement.exception.IllegalStateTransitionException;
import com.financialsurveillance.casemanagement.service.CaseService;
import com.financialsurveillance.events.CaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseController.class)
public class CaseControllerTest {

    @MockitoBean
    private CaseService caseService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CaseDetailResponse detailResponse;
    private CaseSummaryResponse summaryResponse;

    @BeforeEach
    void setUp(){
        detailResponse = CaseDetailResponse.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .alertId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .advisorId("ADV-001")
                .status(CaseStatus.OPEN)
                .assignedTo("sarah.chen")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .closedAt(null)
                .auditTrails(List.of())
                .build();

        summaryResponse = CaseSummaryResponse.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .alertId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .advisorId("ADV-001")
                .status(CaseStatus.OPEN)
                .assignedTo("sarah.chen")
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .closedAt(null)
                .build();

    }
    private TransitionRequest getTransitionRequest(CaseStatus status){
        return TransitionRequest.builder()
                .newStatus(status)
                .performedBy("sarah.chen")
                .build();
    }
    private AssignRequest getAssignRequest(String assignedTo){
        return AssignRequest.builder()
                .assignedTo(assignedTo)
                .performedBy("sarah.chen")
                .build();
    }

    @Test
    void getAllCases_shouldReturnPageOfCases() throws Exception {
        Page<CaseSummaryResponse> responsePage = new PageImpl<>(List.of(summaryResponse));
        when(caseService.getAllCases(any(), any(), any(), any()))
                .thenReturn(responsePage);

        mockMvc.perform(
                        get("/api/v1/cases")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].advisorId").value("ADV-001"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].assignedTo").value("sarah.chen"));

    }

    @Test
    void getCaseDetail_shouldReturnCaseDetailResponse() throws Exception {
        when(caseService.getCaseById(any()))
                .thenReturn(detailResponse);
        mockMvc.perform(
                        get("/api/v1/cases/11111111-1111-1111-1111-111111111111")

                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.advisorId").value("ADV-001"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getCaseDetail_shouldReturn404_whenCaseNotFound() throws Exception {
        when(caseService.getCaseById(any()))
                .thenThrow(new CaseNotFoundException(detailResponse.getId()));

        mockMvc.perform(
                        get("/api/v1/cases/11111111-1111-1111-1111-111111111111")
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void changeStatus_shouldReturnUpdatedCase() throws Exception {
        TransitionRequest request = getTransitionRequest(CaseStatus.IN_REVIEW);
        when(caseService.transitionStatus(any(), any()))
                .thenReturn(detailResponse);
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.advisorId").value("ADV-001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTo").value("sarah.chen"));

    }
    @Test
    void changeStatus_shouldReturn404_whenCaseNotFound() throws Exception {
        TransitionRequest request = getTransitionRequest(CaseStatus.IN_REVIEW);
        when(caseService.transitionStatus(any(), any()))
                .thenThrow(new CaseNotFoundException(detailResponse.getId()));

        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());
    }
    @Test
    void changeStatus_shouldReturn409_whenIllegalTransition() throws Exception {
        TransitionRequest request = getTransitionRequest(CaseStatus.CLOSED_NO_ACTION);
        when(caseService.transitionStatus(any(), any()))
                .thenThrow(new IllegalStateTransitionException(detailResponse.getId()));

        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict());

    }
    @Test
    void changeStatus_shouldReturn400_whenRequestBodyInvalid() throws Exception {
        TransitionRequest request = getTransitionRequest(null);
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignCase_shouldReturnUpdatedCase() throws Exception {
        AssignRequest request = getAssignRequest("sarah.chen");
        when(caseService.assign(any(), any()))
                .thenReturn(detailResponse);
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.advisorId").value("ADV-001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTo").value("sarah.chen"));

    }
    @Test
    void assignCase_shouldReturnUpdatedCase_whenUnassigned() throws Exception {
        AssignRequest request = getAssignRequest(null);
        when(caseService.assign(any(), any()))
                .thenReturn(detailResponse);
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.advisorId").value("ADV-001"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedTo").value("sarah.chen"));

    }
    @Test
    void assignCase_shouldReturn404_whenCaseNotFound () throws Exception {
        AssignRequest request = getAssignRequest("sarah.chen");
        when(caseService.assign(any(), any()))
                .thenThrow(new CaseNotFoundException(detailResponse.getId()));
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isNotFound());

    }
    @Test
    void assignCase_shouldReturn400_whenRequestBodyInvalid () throws Exception {
        AssignRequest invalidRequest = AssignRequest.builder()
                .assignedTo("sarah.chen")
                .performedBy("")
                .build();
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                )
                .andExpect(status().isBadRequest());

    }
}
