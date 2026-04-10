package com.nazir.aiinvoice.config;

import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiStrategyConfig {

    @Value("${ai.provider:local}")
    private String aiProvider;

    @Bean
    @Primary
    public AiExtractionStrategy aiExtractionStrategy(@Qualifier("localExtractionService") AiExtractionStrategy local, @Qualifier("openAiExtractionService") AiExtractionStrategy openai, @Qualifier("ollamaExtractionService") AiExtractionStrategy ollama) {
        return switch (aiProvider.toLowerCase()) {
            case "local", "regex" -> local;
            case "openai" -> openai;
            case "ollama" -> ollama;
            default -> local;
        };
    }
}
