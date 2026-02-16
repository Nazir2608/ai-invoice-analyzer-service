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

@Service("openAiExtractionService")
@RequiredArgsConstructor
@Slf4j
public class OpenAiExtractionService implements AiExtractionStrategy {

    private final InvoiceRepository repository;
    private final DocumentTextExtractor documentTextExtractor;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Value("${ai.openai.api-key:}")
    private String apiKey;
    @Value("${ai.openai.model:gpt-3.5-turbo}")
    private String model;
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        log.info("event=openai_extraction_started invoiceId={}", invoiceId);
        if (apiKey == null || apiKey.isBlank()) {
            log.error("event=openai_api_key_missing invoiceId={}", invoiceId);
            markFailed(invoice);
            return;
        }
        try {
            String text = documentTextExtractor.extractText(invoice.getFileUrl());
            if (text == null || text.isBlank()) {
                log.warn("event=empty_text invoiceId={}", invoiceId);
                markFailed(invoice);
                return;
            }
            invoice.setExtractedRawText(text);
            String jsonResponse = callOpenAi(text);
            updateInvoiceFromJson(invoice, jsonResponse);
            invoice.setStatus(InvoiceStatus.COMPLETED);
            repository.save(invoice);
            log.info("event=openai_extraction_completed invoiceId={}", invoiceId);
        } catch (Exception e) {
            log.error("event=openai_extraction_failed invoiceId={}", invoiceId, e);
            markFailed(invoice);
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

                Text:
                """ + text;
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that extracts data from invoices."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1
        );

        return restClientBuilder.build()
                .post()
                .uri(OPENAI_URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    private void updateInvoiceFromJson(Invoice invoice, String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").get(0).path("message").path("content");
        if (contentNode.isMissingNode()) {
            throw new RuntimeException("Invalid OpenAI response format");
        }

        String content = contentNode.asText();
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
