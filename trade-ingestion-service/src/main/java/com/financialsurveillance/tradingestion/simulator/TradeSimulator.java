package com.financialsurveillance.tradingestion.simulator;

import com.financialsurveillance.tradingestion.domain.TradeType;
import com.financialsurveillance.tradingestion.dto.TradeRequest;
import com.financialsurveillance.tradingestion.exception.DuplicateTradeException;
import com.financialsurveillance.tradingestion.exception.InvalidTradeException;
import com.financialsurveillance.tradingestion.service.TradeIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
public class TradeSimulator {

    private final TradeIngestionService tradeIngestionService;
    private static final List<String> ADVISOR_IDS = List.of(
            "ADV-1001", "ADV-1002", "ADV-1003", "ADV-1004"
    );

    private static final List<String> ACCOUNT_IDS = List.of(
            "ACC-2001", "ACC-2002", "ACC-2003", "ACC-2004"
    );

    private static final List<String> CLIENT_IDS = List.of(
            "CL-3001", "CL-3002", "CL-3003", "CL-3004"
    );

    private static final List<String> SYMBOLS = List.of(
            "AAPL", "MSFT", "JPM", "GS", "TSLA",
            "AMZN", "GOOGL", "META", "BAC", "WFC"
    );

    private static final List<String> EXCHANGES = List.of(
            "NYSE", "NASDAQ"
    );

    private static final List<String> SOURCE_SYSTEMS = List.of(
            "OMS", "EMS", "TRADING_DESK_UI"
    );

    private final Random random = new Random();

    public TradeSimulator(TradeIngestionService tradeIngestionService) {
        this.tradeIngestionService = tradeIngestionService;
    }

    public TradeRequest generateTrade() {

        String advisorId = randomFrom(ADVISOR_IDS);
        String accountId = randomFrom(ACCOUNT_IDS);
        String clientId = randomFrom(CLIENT_IDS);
        String symbol = randomFrom(SYMBOLS);
        TradeType tradeType = randomTradeType();
        BigDecimal quantity = randomQuantity();
        BigDecimal price = randomPrice();
        ZonedDateTime tradeTimestamp = ZonedDateTime.now().minusMinutes(random.nextInt(60 * 24 * 3));

        return TradeRequest.builder()
                .tradeId("TRD-" + UUID.randomUUID())
                .advisorId(advisorId)
                .accountId(accountId)
                .clientId(clientId)
                .symbol(symbol)
                .tradeType(tradeType)
                .quantity(quantity)
                .price(price)
                .currency("USD")
                .exchange(randomFrom(EXCHANGES))
                .tradeTimestamp(tradeTimestamp)
                .sourceSystem(randomFrom(SOURCE_SYSTEMS))
                .sourceSystemId("SRC-" + UUID.randomUUID())
                .build();
    }

    @Scheduled(fixedDelay = 5000)
    public void simulateTrade(){

        try {
            TradeRequest tradeRequest = generateTrade();
            tradeIngestionService.processTrade(tradeRequest);

        } catch (DuplicateTradeException e) {
            log.warn("Duplicate trade skipped tradeId={}", e.getMessage());
        } catch (InvalidTradeException e){
            log.warn("Invalid trade skipped reason={}", e.getMessage());
        }
    }

    private String randomFrom(List<String> values) {
        return values.get(random.nextInt(values.size()));
    }

    private TradeType randomTradeType() {
        TradeType[] types = TradeType.values();
        return types[random.nextInt(types.length)];
    }

    private BigDecimal randomQuantity() {
        int value = random.nextInt(50000) + 1; // scaled value
        return BigDecimal.valueOf(value, 2);   // scale = 2 → e.g. 123.45
    }

    private BigDecimal randomPrice() {
        int value = random.nextInt(5000000) + 5000; // 50.00 → 500.00
        return BigDecimal.valueOf(value, 2);        // scale = 2
    }
}