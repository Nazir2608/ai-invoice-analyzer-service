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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("ollamaExtractionService")
@RequiredArgsConstructor
@Slf4j
public class OllamaExtractionService implements AiExtractionStrategy {

    private final InvoiceRepository repository;
    private final DocumentTextExtractor documentTextExtractor;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final InvoiceRiskService invoiceRiskService;
    private final InvoiceJsonMapper invoiceJsonMapper;
    private final InvoiceEventService invoiceEventService;

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ai.ollama.model:llama3}")
    private String model;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        invoice.setStatus(InvoiceStatus.PROCESSING);
        repository.save(invoice);
        log.info("event=ollama_extraction_started invoiceId={}", invoiceId);
        invoiceEventService.record(invoiceId, InvoiceEventType.AI_STARTED, "Ollama extraction started");
        try {
            String text = documentTextExtractor.extractText(invoice.getFileUrl());
            if (text == null || text.isBlank()) {
                log.warn("event=empty_text invoiceId={}", invoiceId);
                markFailed(invoice);
                return;
            }
            invoice.setExtractedRawText(text);
            invoiceEventService.record(invoiceId, InvoiceEventType.TEXT_EXTRACTED, "Text extracted from document");
            String jsonResponse = callOllama(text);
            updateInvoiceFromJson(invoice, jsonResponse);
            String summary = generateSummary(text);
            invoice.setAiSummary(summary);
            invoice.setStatus(InvoiceStatus.AI_COMPLETED);
            repository.save(invoice);
            invoiceRiskService.applyRiskChecks(invoice);
            invoice.setStatus(InvoiceStatus.RISK_ANALYZED);
            repository.save(invoice);
            invoice.setStatus(InvoiceStatus.COMPLETED);
            repository.save(invoice);
            log.info("event=ollama_extraction_completed invoiceId={}", invoiceId);
        } catch (AiExtractionException e) {
            log.error("event=ollama_extraction_failed_invalid_response invoiceId={} message={}", invoiceId, e.getMessage());
            invoiceEventService.record(invoiceId, InvoiceEventType.PROCESSING_FAILED, "Ollama extraction failed: " + e.getMessage());
            markFailed(invoice);
        } catch (Exception e) {
            log.error("event=ollama_extraction_failed invoiceId={}", invoiceId, e);
            invoiceEventService.record(invoiceId, InvoiceEventType.PROCESSING_FAILED, "Ollama extraction failed: " + e.getMessage());
            markFailed(invoice);
        }
    }

    private String callOllama(String text) {
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
                - lineItems: array of objects with:
                  - description
                  - quantity
                  - unitPrice
                  - lineTotal
                - confidenceScore (integer 0-100 indicating overall extraction confidence)

                Text:
                """ + text;

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "format", "json"
        );

        String responseBody = restClientBuilder.build()
                .post()
                .uri(ollamaUrl + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return responseBody;
    }

    private void updateInvoiceFromJson(Invoice invoice, String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode responseNode = root.path("response");
        
        String content;
        if (responseNode.isMissingNode()) {
            if (root.has("vendorName")) {
                content = responseBody;
            } else {
                throw new AiExtractionException("Invalid Ollama response format");
            }
        } else {
            content = responseNode.asText();
        }

        String cleaned = invoiceJsonMapper.cleanContent(content);
        JsonNode data = objectMapper.readTree(cleaned);

        invoiceJsonMapper.applyBasicFields(invoice, data);

        if (data.has("lineItems")) {
            invoiceJsonMapper.applyLineItems(invoice, data.get("lineItems"));
        }
    }

    private String generateSummary(String text) {
        try {
            String prompt = """
                    Summarize this invoice in 3 sentences:
                    - What service or product is being billed
                    - Total amount
                    - Payment due date

                    Text:
                    """ + text;

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false
            );

            String responseBody = restClientBuilder.build()
                    .post()
                    .uri(ollamaUrl + "/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode responseNode = root.path("response");
            if (responseNode.isMissingNode()) {
                return null;
            }
            return responseNode.asText().trim();
        } catch (Exception e) {
            log.warn("event=ollama_summary_failed", e);
            return null;
        }
    }

    private void markFailed(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.FAILED);
        repository.save(invoice);
    }
}
