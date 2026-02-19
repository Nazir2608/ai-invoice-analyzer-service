package com.nazir.aiinvoice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AI Invoice Analyzer API", version = "v1",
                description = "REST API for uploading, analyzing, and managing invoices with AI-assisted extraction.",
                contact = @Contact(name = "AI Invoice Analyzer Team", email = "support@ai-invoice.local"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local environment")
        }
)
public class OpenApiConfig {
    @Bean
    public GroupedOpenApi invoiceApi() {
        return GroupedOpenApi.builder().group("invoices").pathsToMatch("/api/invoices/**").build();
    }
}

