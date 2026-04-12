package com.financialsurveillance.casemanagement;

import org.springframework.boot.SpringApplication;

public class TestCaseManagementServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(CaseManagementServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
