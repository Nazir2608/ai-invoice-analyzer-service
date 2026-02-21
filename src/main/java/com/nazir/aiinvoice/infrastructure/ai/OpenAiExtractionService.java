package com.nazir.aiinvoice.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazir.aiinvoice.application.mapper.InvoiceJsonMapper;
import com.nazir.aiinvoice.application.service.InvoiceEventService;
import com.nazir.aiinvoice.application.service.InvoiceRiskService;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceEventType;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import com.nazir.aiinvoice.exception.AiExtractionException;
import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("openAiExtractionService")
@RequiredArgsConstructor
@Slf4j
public class OpenAiExtractionService implements AiExtractionStrategy {

    private final InvoiceRepository repository;
    private final DocumentTextExtractor documentTextExtractor;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final InvoiceRiskService invoiceRiskService;
    private final InvoiceJsonMapper invoiceJsonMapper;
    private final InvoiceEventService invoiceEventService;

    @Value("${ai.openai.api-key:}")
    private String apiKey;
    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String model;
    @Value("${ai.openai.url:https://api.openai.com/v1/chat/completions}")
    private String openAiUrl;
    @Value("${ai.openai.temperature:0.1}")
    private double temperature;

    @Override
    @Retry(name = "aiService", fallbackMethod = "fallback")
    @CircuitBreaker(name = "aiService", fallbackMethod = "fallback")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        invoice.setStatus(InvoiceStatus.PROCESSING);
        repository.save(invoice);
        log.info("event=openai_extraction_started invoiceId={}", invoiceId);
        if (apiKey == null || apiKey.isBlank()) {
            log.error("event=openai_api_key_missing invoiceId={}", invoiceId);
            markFailed(invoice);
            return;
        }
        try {
            invoiceEventService.record(invoiceId, InvoiceEventType.AI_STARTED, "OpenAI extraction started");
            String text = documentTextExtractor.extractText(invoice.getFileUrl());
            if (text == null || text.isBlank()) {
                log.warn("event=empty_text invoiceId={}", invoiceId);
                markFailed(invoice);
                return;
            }
            invoice.setExtractedRawText(text);
            invoiceEventService.record(invoiceId, InvoiceEventType.TEXT_EXTRACTED, "Text extracted from document");
            String jsonResponse = callOpenAi(text);
            updateInvoiceFromJson(invoice, jsonResponse);
            invoice.setStatus(InvoiceStatus.AI_COMPLETED);
            repository.save(invoice);
            invoiceRiskService.applyRiskChecks(invoice);
            invoice.setStatus(InvoiceStatus.RISK_ANALYZED);
            repository.save(invoice);
            invoice.setStatus(InvoiceStatus.COMPLETED);
            repository.save(invoice);
            log.info("event=openai_extraction_completed invoiceId={}", invoiceId);
            invoiceEventService.record(invoiceId, InvoiceEventType.AI_COMPLETED, "OpenAI extraction completed");
        } catch (AiExtractionException e) {
            log.error("event=openai_extraction_failed_invalid_response invoiceId={} message={}", invoiceId, e.getMessage());
            invoiceEventService.record(invoiceId, InvoiceEventType.PROCESSING_FAILED, "OpenAI extraction failed: " + e.getMessage());
            markFailed(invoice);
        } catch (Exception e) {
            log.error("event=openai_extraction_failed invoiceId={}", invoiceId, e);
            invoiceEventService.record(invoiceId, InvoiceEventType.PROCESSING_FAILED, "OpenAI extraction failed: " + e.getMessage());
            markFailed(invoice);
        }
    }

    public void fallback(UUID invoiceId, Throwable ex) {
        log.error("event=openai_extraction_failed_fallback invoiceId={} reason={}", invoiceId, ex.getMessage());
        Invoice invoice = repository.findById(invoiceId).orElse(null);
        if (invoice != null) {
            invoice.setStatus(InvoiceStatus.FAILED);
            repository.save(invoice);
            invoiceEventService.record(invoiceId, InvoiceEventType.PROCESSING_FAILED, "OpenAI extraction fallback executed: " + ex.getMessage());
        }
    }

    private String callOpenAi(String text) {
        String prompt = """
                Extract the following fields from the invoice text below and return ONLY valid JSON. 
                Do not include markdown formatting (like ```json).
                Fields:
                - vendorName
                - vendorEmail
                - vendorAddress
                - billToName
                - billToAddress
                - invoiceNumber
                - invoiceDate (YYYY-MM-DD)
                - dueDate (YYYY-MM-DD)
                - subtotal (number)
                - taxAmount (number)
                - totalAmount (number)
                - currency (ISO code)
                - confidenceScore (integer 0-100 indicating overall extraction confidence)

                Text:
                """ + text;
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that extracts data from invoices."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", temperature
        );

        return restClientBuilder.build()
                .post()
                .uri(openAiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    private void updateInvoiceFromJson(Invoice invoice, String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choicesNode = root.path("choices");
        if (!choicesNode.isArray() || choicesNode.isEmpty()) {
            throw new AiExtractionException("Invalid OpenAI response: missing choices");
        }
        JsonNode contentNode = choicesNode.get(0).path("message").path("content");
        if (contentNode.isMissingNode()) {
            throw new AiExtractionException("Invalid OpenAI response: missing content");
        }
        String content = contentNode.asText();
        String cleaned = invoiceJsonMapper.cleanContent(content);
        JsonNode data = objectMapper.readTree(cleaned);
        invoiceJsonMapper.applyBasicFields(invoice, data);
    }

    private void markFailed(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.FAILED);
        repository.save(invoice);
    }
}
