package com.financialsurveillance.tradingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradeIngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeIngestionServiceApplication.class, args);
	}

}
