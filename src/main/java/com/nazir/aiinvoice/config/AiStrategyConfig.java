package com.nazir.aiinvoice.config;

import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiStrategyConfig {

    @Value("${ai.provider:mock}")
    private String aiProvider;

    @Bean
    @Primary
    public AiExtractionStrategy aiExtractionStrategy(
            @Qualifier("mockExtractionService") AiExtractionStrategy mock,
            @Qualifier("localExtractionService") AiExtractionStrategy local,
            @Qualifier("openAiExtractionService") AiExtractionStrategy openai,
            @Qualifier("hybridExtractionService") AiExtractionStrategy hybrid,
            @Qualifier("ollamaExtractionService") AiExtractionStrategy ollama
    ) {
        return switch (aiProvider.toLowerCase()) {
            case "local", "regex" -> local;
            case "openai" -> openai;
            case "hybrid" -> hybrid;
            case "ollama" -> ollama;
            default -> mock;
        };
    }
}
