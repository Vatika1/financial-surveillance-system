package com.financialsurveillance.casemanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.casemanagement.dto.AssignRequest;
import com.financialsurveillance.casemanagement.dto.CaseDetailResponse;
import com.financialsurveillance.casemanagement.dto.CaseSummaryResponse;
import com.financialsurveillance.casemanagement.dto.TransitionRequest;
import com.financialsurveillance.casemanagement.service.CaseService;
import com.financialsurveillance.events.CaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CaseController.class)
public class CaseControllerTest {

    private static final UUID CASE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

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
        Page<CaseSummaryResponse> responsePage = new PageImpl<>(List.of(summaryResponse),
                PageRequest.of(0, 10), 1);

        when(caseService.getAllCases(any(), any(), any(), any()))
                .thenReturn(responsePage);

        mockMvc.perform(
                        get("/api/v1/cases")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.content[0].advisorId").value("ADV-001"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].assignedTo").value("sarah.chen"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.number").value(0));

        verify(caseService).getAllCases(any(), any(), any(), any(Pageable.class));
    }

    @Test
    void getAllCases_shouldPassPagingParamsToService() throws Exception{
        Page<CaseSummaryResponse> responsePage = new PageImpl<>(List.of(), PageRequest.of(1, 5), 0);
        when(caseService.getAllCases(any(), any(), any(), any(Pageable.class)))
                .thenReturn(responsePage);

        mockMvc.perform(
                        get("/api/v1/cases")
                                .param("page", "1")
                                .param("size", "5")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.number").value(1));

        verify(caseService).getAllCases(any(), any(), any(), argThat((Pageable p) ->
                p.getPageNumber() == 1 && p.getPageSize() == 5));
    }

    @Test
    void getCaseDetail_shouldReturn400_whenIdNotUuid() throws Exception{
        mockMvc.perform(
                        get("/api/v1/cases/not-a-uuid")

                )
                .andExpect(status().isBadRequest());

        verify(caseService, never()).getCaseById(any());
    }

    @Test
    void assignCase_shouldReturn400_whenJsonMalformed() throws Exception{
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{ \"assignedTo\": ")
                )
                .andExpect(status().isBadRequest());

        verify(caseService, never()).assign(any(), any());

    }

    @Test
    void getAllCases_shouldReturn400_whenStatusNotValidEnum() throws Exception{
        mockMvc.perform(
                        get("/api/v1/cases")
                                .param("status", "BANANA")
                )
                .andExpect(status().isBadRequest());

        verify(caseService, never()).getAllCases(any(), any(), any(), any(Pageable.class));
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
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.alertId").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.assignedTo").value("sarah.chen"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.closedAt").value(nullValue()))
                .andExpect(jsonPath("$.auditTrails").isEmpty())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
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

        verify(caseService).transitionStatus(eq(CASE_ID), argThat(r ->
                r.getNewStatus() == CaseStatus.IN_REVIEW && "sarah.chen".equals(r.getPerformedBy())));
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
    void assignCase_shouldReturn400_whenRequestBodyInvalid () throws Exception {
        AssignRequest invalidRequest = AssignRequest.builder()
                .assignedTo("sarah.chen")
                .performedBy("")
                .build();
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/assign")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                )
                .andExpect(status().isBadRequest());

    }

}
