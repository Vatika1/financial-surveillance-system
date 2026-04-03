package com.financialsurveillance.tradingestion.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeType;
import com.financialsurveillance.tradingestion.controller.TradeController;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.service.TradeIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TradeIngestionService tradeService;

    @Autowired
    private ObjectMapper objectMapper;

    private TradeRequest request;

    @BeforeEach
    void setUp(){
        request = TradeRequest.builder()
                .tradeId("TRD-001")
                .advisorId("ADV-001")
                .accountId("ACC-001")
                .clientId("CLT-001")
                .symbol("AAPL")
                .tradeType(TradeType.BUY)
                .quantity(BigDecimal.valueOf(100))
                .price(BigDecimal.valueOf(150.00))
                .currency("USD")
                .exchange("NASDAQ")
                .tradeTimestamp(ZonedDateTime.now().minusMinutes(5))
                .sourceSystem("ETRADE")
                .sourceSystemId("SRC-001")
                .build();
    }

    @Test
    void shouldReturn500AndGenericMessage_whenUnhandledExceptionOccurs() throws Exception{

        when(tradeService.processTrade(any()))
                .thenThrow(new RuntimeException());

        mockMvc.perform(post("/api/trades")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL SERVER ERROR"))
                .andExpect(jsonPath("$.message").value("Something went wrong"))
                .andExpect(jsonPath("$.path").value("/api/trades"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400InvalidTradeException_whenUnhandledExceptionOccurs() throws Exception{
        when(tradeService.processTrade(any()))
                .thenThrow(new InvalidTradeException(request.getTradeId(), ""));

        mockMvc.perform(post("/api/trades")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad request"))
                .andExpect(jsonPath("$.message").value("Invalid trade [" + request.getTradeId() + "]: "))
                .andExpect(jsonPath("$.path").value("/api/trades"))
                .andExpect(jsonPath("$.timestamp").exists());

    }

    @Test
    void shouldReturn409DuplicateTradeException_whenUnhandledExceptionOccurs() throws Exception{
        when(tradeService.processTrade(any()))
                .thenThrow(new DuplicateTradeException(request.getTradeId()));

        mockMvc.perform(post("/api/trades")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Trade already exists with ID: "+ request.getTradeId()))
                .andExpect(jsonPath("$.path").value("/api/trades"))
                .andExpect(jsonPath("$.timestamp").exists());

    }
}
