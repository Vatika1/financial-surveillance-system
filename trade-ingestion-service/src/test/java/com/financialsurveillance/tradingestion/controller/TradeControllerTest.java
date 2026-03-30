package com.financialsurveillance.tradingestion.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialsurveillance.events.TradeStatus;
import com.financialsurveillance.events.TradeType;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.dto.TradeResponse;
import com.financialsurveillance.tradingestion.exception.DuplicateTradeException;
import com.financialsurveillance.tradingestion.exception.InvalidTradeException;
import com.financialsurveillance.tradingestion.service.TradeIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TradeController.class)
public class TradeControllerTest {

    @MockitoBean
    private TradeIngestionService tradeService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private TradeResponse response;
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

        response = TradeResponse.builder()
                .id(UUID.randomUUID())
                .tradeId("TRD-001")
                .createdAt(ZonedDateTime.now())
                .status(TradeStatus.RECEIVED)
                .build();
    }

    @Test
    void submitTrade_ShouldReturnTrade_WhenCreated() throws Exception {
        when(tradeService.processTrade(any()))
                .thenReturn(response);
        mockMvc.perform(
                        post("/api/trades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tradeId").value("TRD-001"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void shouldNotSubmitTrade_ShouldReturn409_DuplicateTrade() throws Exception {
        when(tradeService.processTrade(any()))
                .thenThrow(new DuplicateTradeException(request.getTradeId()));

        mockMvc.perform(
                        post("/api/trades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldNotSubmitTrade_ShouldReturn400_InvalidTrade() throws Exception {
        when(tradeService.processTrade(any()))
                .thenThrow(new InvalidTradeException(request.getTradeId(), "Symbol not recognized"));

        mockMvc.perform(
                        post("/api/trades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
