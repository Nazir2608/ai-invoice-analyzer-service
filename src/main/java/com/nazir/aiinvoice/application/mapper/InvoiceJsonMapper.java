package com.nazir.aiinvoice.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceJsonMapper {

    public void applyBasicFields(Invoice invoice, JsonNode data) {
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

    public void applyLineItems(Invoice invoice, JsonNode lineItemsNode) {
        if (lineItemsNode == null || !lineItemsNode.isArray()) {
            return;
        }
        invoice.getItems().clear();
        for (JsonNode itemNode : lineItemsNode) {
            String description = getText(itemNode, "description");
            BigDecimal quantity = getDecimal(itemNode, "quantity");
            BigDecimal unitPrice = getDecimal(itemNode, "unitPrice");
            BigDecimal lineTotal = getDecimal(itemNode, "lineTotal");

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .name(description)
                    .quantity(quantity)
                    .price(unitPrice)
                    .lineTotal(lineTotal)
                    .build();

            invoice.getItems().add(item);
        }
    }

    public String cleanContent(String content) {
        String result = content;
        if (result.startsWith("```json")) {
            result = result.substring(7);
        }
        if (result.startsWith("```")) {
            result = result.substring(3);
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3);
        }
        return result.trim();
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
}

