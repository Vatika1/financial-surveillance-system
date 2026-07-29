package com.financialsurveillance.casemanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI caseManagementOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Case Management API")
                .version("1.0.0")
                .description("Opens and manages investigation cases created from persisted alerts."));
    }
}
