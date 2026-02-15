package com.nazir.aiinvoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiInvoiceAnalyzerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiInvoiceAnalyzerServiceApplication.class, args);
    }
}