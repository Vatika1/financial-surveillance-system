package com.financialsurveillance.casemanagement.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.casemanagement.controller.CaseController;
import com.financialsurveillance.casemanagement.dto.TransitionRequest;
import com.financialsurveillance.casemanagement.service.CaseService;
import com.financialsurveillance.events.CaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaseController.class)
@AutoConfigureObservability
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaseService caseService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID CASE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private TransitionRequest request;

    @BeforeEach
    void setUp() {
        request = TransitionRequest.builder()
                .newStatus(CaseStatus.IN_REVIEW)
                .performedBy("sarah.chen")
                .build();
    }

    @Test
    void getCaseDetail_shouldReturn404_whenCaseNotFound() throws Exception {
        when(caseService.getCaseById(any()))
                .thenThrow(new CaseNotFoundException(CASE_ID));

        mockMvc.perform(
                        get("/api/v1/cases/11111111-1111-1111-1111-111111111111")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Case not found for id: " + CASE_ID))
                .andExpect(jsonPath("$.path").value("/api/v1/cases/11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.timestamp").exists());

    }

    @Test
    void shouldReturn409_whenIllegalStateTransition() throws Exception{

        when(caseService.transitionStatus(any(), any()))
                .thenThrow(new IllegalStateTransitionException(CASE_ID));

        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Illegal transition for case: " + CASE_ID))
                .andExpect(jsonPath("$.path").value("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400_whenValidationFails() throws Exception{
        request.setPerformedBy("");
        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn500_whenUnexpectedException() throws Exception{

        when(caseService.transitionStatus(any(), any()))
                .thenThrow(new RuntimeException());

        mockMvc.perform(
                        post("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL SERVER ERROR"))
                .andExpect(jsonPath("$.message").value("Something went wrong"))
                .andExpect(jsonPath("$.path").value("/api/v1/cases/11111111-1111-1111-1111-111111111111/transition"))
                .andExpect(jsonPath("$.timestamp").exists());

    }

}
