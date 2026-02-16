package com.nazir.aiinvoice.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
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

    @Value("${ai.ollama.url:http://localhost:11434}")
    private String ollamaUrl;
    
    @Value("${ai.ollama.model:llama3}")
    private String model;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        log.info("event=ollama_extraction_started invoiceId={}", invoiceId);
        try {
            String text = documentTextExtractor.extractText(invoice.getFileUrl());
            if (text == null || text.isBlank()) {
                log.warn("event=empty_text invoiceId={}", invoiceId);
                markFailed(invoice);
                return;
            }
            invoice.setExtractedRawText(text);
            String jsonResponse = callOllama(text);
            updateInvoiceFromJson(invoice, jsonResponse);
            invoice.setStatus(InvoiceStatus.COMPLETED);
            repository.save(invoice);
            log.info("event=ollama_extraction_completed invoiceId={}", invoiceId);
        } catch (Exception e) {
            log.error("event=ollama_extraction_failed invoiceId={}", invoiceId, e);
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
             // Sometimes Ollama might return the content directly or in a different structure depending on version/model
             // But standard Ollama /api/generate returns "response" field
             // If not found, let's try to parse root as the response if it's not a structured API response
             if (root.has("vendorName")) {
                 content = responseBody; 
             } else {
                 throw new RuntimeException("Invalid Ollama response format: " + responseBody);
             }
        } else {
            content = responseNode.asText();
        }

        // Clean up markdown if present
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }

        JsonNode data = objectMapper.readTree(content.trim());

        if (data.has("vendorName")) invoice.setVendorName(getText(data, "vendorName"));
        if (data.has("vendorEmail")) invoice.setVendorEmail(getText(data, "vendorEmail"));
        if (data.has("vendorAddress")) invoice.setVendorAddress(getText(data, "vendorAddress"));
        if (data.has("billToName")) invoice.setBillToName(getText(data, "billToName"));
        if (data.has("billToAddress")) invoice.setBillToAddress(getText(data, "billToAddress"));
        if (data.has("invoiceNumber")) invoice.setInvoiceNumber(getText(data, "invoiceNumber"));
        if (data.has("currency")) invoice.setCurrency(getText(data, "currency"));

        if (data.has("invoiceDate")) invoice.setInvoiceDate(getDate(data, "invoiceDate"));
        if (data.has("dueDate")) invoice.setDueDate(getDate(data, "dueDate"));

        if (data.has("subtotal")) invoice.setSubtotal(getDecimal(data, "subtotal"));
        if (data.has("taxAmount")) invoice.setTaxAmount(getDecimal(data, "taxAmount"));
        if (data.has("totalAmount")) invoice.setTotalAmount(getDecimal(data, "totalAmount"));
    }

    private String getText(JsonNode node, String field) {
        return node.path(field).isNull() ? null : node.path(field).asText();
    }

    private LocalDate getDate(JsonNode node, String field) {
        String text = getText(node, field);
        if (text == null) return null;
        try {
            return LocalDate.parse(text, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", text);
            return null;
        }
    }

    private BigDecimal getDecimal(JsonNode node, String field) {
        if (node.path(field).isNull()) return null;
        try {
            return new BigDecimal(node.path(field).asText());
        } catch (Exception e) {
            log.warn("Failed to parse decimal: {}", node.path(field).asText());
            return null;
        }
    }

    private void markFailed(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.FAILED);
        repository.save(invoice);
    }
}
