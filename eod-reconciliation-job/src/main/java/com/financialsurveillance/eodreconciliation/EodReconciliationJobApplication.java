package com.financialsurveillance.eodreconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

@SpringBootApplication
public class EodReconciliationJobApplication {

    public static void main(String[] args) {
        boolean hasTradeDate = Arrays.stream(args).anyMatch(a -> a.startsWith("tradeDate="));
        String[] jobArgs = hasTradeDate ? args : append(args,
                "tradeDate=" + LocalDate.now(ZoneId.of("America/New_York")));
        System.exit(SpringApplication.exit(SpringApplication.run(EodReconciliationJobApplication.class, jobArgs)));
    }

    private static String[] append(String[] args, String extra) {
        String[] result = Arrays.copyOf(args, args.length + 1);
        result[args.length] = extra;
        return result;
    }
}