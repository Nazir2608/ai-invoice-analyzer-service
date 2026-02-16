package com.nazir.aiinvoice.infrastructure.ai;

import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("localExtractionService")
@RequiredArgsConstructor
@Slf4j
public class LocalRegexExtractionService implements AiExtractionStrategy {

    private final InvoiceRepository repository;
    private final DocumentTextExtractor documentTextExtractor;

    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("(?i)invoice[^:0-9]*[:#]?\\s*([a-zA-Z0-9\\-]+)");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("(?i)(total|amount\\s*due|balance\\s*due)\\s*[:.]?\\s*[$€£]?\\s*([\\d,]+\\.?\\d{0,2})");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(?:invoice\\s*)?date\\s*[:.]?\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("(?i)(USD|EUR|GBP|INR|CAD|AUD)");
    private static final Pattern TAX_PATTERN = Pattern.compile("(?i)(tax|vat|gst).*?[:]+?\\s*[$€£]?\\s*([\\d,]+\\.?\\d{0,2})");
    private static final Pattern SUBTOTAL_PATTERN = Pattern.compile("(?i)(subtotal|sub\\s*total)\\s*[:.]?\\s*[$€£]?\\s*([\\d,]+\\.?\\d{0,2})");

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        log.info("event=invoice_extraction_started invoiceId={}", invoiceId);
        try {
            String text = documentTextExtractor.extractText(invoice.getFileUrl());
            if (text == null || text.isBlank()) {
                log.warn("event=empty_text invoiceId={}", invoiceId);
                markFailed(invoice);
                return;
            }
            invoice.setExtractedRawText(text);
            parseAndPopulate(invoice, text);
            invoice.setStatus(InvoiceStatus.COMPLETED);
            repository.save(invoice);
            log.info("event=invoice_extraction_completed invoiceId={} vendor={} total={}", invoiceId, invoice.getVendorName(), invoice.getTotalAmount());
        } catch (Exception ex) {
            log.error("event=invoice_extraction_failed invoiceId={}", invoiceId, ex);
            markFailed(invoice);
        }
    }

    private void parseAndPopulate(Invoice invoice, String text) {
        extractInvoiceNumber(invoice, text);
        extractTotal(invoice, text);
        extractSubtotal(invoice, text);
        extractTax(invoice, text);
        extractDate(invoice, text);
        extractVendor(invoice, text);
        extractEmail(invoice, text);
        extractCurrency(invoice, text);
        extractBillTo(invoice, text);
    }

    private void extractInvoiceNumber(Invoice invoice, String text) {
        Matcher matcher = INVOICE_NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(1);
            log.debug("potential_invoice_number_match value={}", value);
            if (!value.equalsIgnoreCase("invoice") && 
                !value.equalsIgnoreCase("date") && 
                !value.equalsIgnoreCase("number") &&
                value.length() > 3 && 
                value.matches(".*\\d.*")) {
                invoice.setInvoiceNumber(value);
                log.info("event=invoice_number_extracted value={}", value);
                break;
            }
        }
    }

    private void extractTotal(Invoice invoice, String text) {
        Matcher matcher = TOTAL_PATTERN.matcher(text);
        String lastAmount = null;
        while (matcher.find()) {
            lastAmount = matcher.group(2).replace(",", "");
        }
        if (lastAmount != null) {
            try {
                invoice.setTotalAmount(new BigDecimal(lastAmount));
                log.info("event=total_extracted value={}", lastAmount);
            } catch (NumberFormatException e) {
                log.warn("event=invalid_amount value={}", lastAmount);
            }
        }
    }

    private void extractSubtotal(Invoice invoice, String text) {
        Matcher matcher = SUBTOTAL_PATTERN.matcher(text);
        if (matcher.find()) {
            String amount = matcher.group(2).replace(",", "");
            try {
                invoice.setSubtotal(new BigDecimal(amount));
            } catch (Exception e) {
                log.warn("event=invalid_subtotal value={}", amount);
            }
        }
    }

    private void extractTax(Invoice invoice, String text) {
        Matcher matcher = TAX_PATTERN.matcher(text);
        if (matcher.find()) {
            String amount = matcher.group(2).replace(",", "");
            try {
                invoice.setTaxAmount(new BigDecimal(amount));
            } catch (Exception e) {
                log.warn("event=invalid_tax value={}", amount);
            }
        }
    }

    private void extractDate(Invoice invoice, String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                LocalDate date = LocalDate.parse(matcher.group(1));
                invoice.setInvoiceDate(date);
                invoice.setDueDate(date.plusDays(30)); 
            } catch (Exception e) {
                log.warn("event=invalid_date value={}", matcher.group(1));
            }
        }
    }

    private void extractVendor(Invoice invoice, String text) {
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.toLowerCase().contains("invoice")) continue;
            if (trimmed.matches("^[\\W_]+$")) continue;
            if (trimmed.endsWith(":")) continue;
            invoice.setVendorName(trimmed);
            log.info("event=vendor_extracted value={}", trimmed);
            break;
        }
    }

    private void extractEmail(Invoice invoice, String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            invoice.setVendorEmail(matcher.group());
        }
    }

    private void extractCurrency(Invoice invoice, String text) {
        Matcher matcher = CURRENCY_PATTERN.matcher(text);
        if (matcher.find()) {
            invoice.setCurrency(matcher.group(1).toUpperCase());
        } else {
            if (text.contains("$")) invoice.setCurrency("USD");
            else if (text.contains("€")) invoice.setCurrency("EUR");
            else if (text.contains("£")) invoice.setCurrency("GBP");
            else if (text.contains("₹")) invoice.setCurrency("INR");
        }
    }

    private void extractBillTo(Invoice invoice, String text) {
        String lowerText = text.toLowerCase();
        int billToIdx = lowerText.indexOf("bill to");
        if (billToIdx != -1) {
            String sub = text.substring(billToIdx + 7).trim();
            if (sub.startsWith(":")) {
                sub = sub.substring(1).trim();
            }
            String[] lines = sub.split("\\r?\\n");
            int lineIdx = 0;
            while (lineIdx < lines.length && lines[lineIdx].trim().isEmpty()) {
                lineIdx++;
            }
            if (lineIdx < lines.length) {
                invoice.setBillToName(lines[lineIdx].trim());
                log.info("event=bill_to_name_extracted value={}", invoice.getBillToName());
                if (lineIdx + 1 < lines.length) {
                    StringBuilder address = new StringBuilder();
                    for (int i = lineIdx + 1; i < Math.min(lineIdx + 4, lines.length); i++) {
                        String line = lines[i].trim();
                        if (line.isEmpty() || line.toLowerCase().startsWith("invoice") || line.toLowerCase().startsWith("description")) break;
                        if (address.length() > 0) address.append(", ");
                        address.append(line);
                    }
                    invoice.setBillToAddress(address.toString());
                    log.info("event=bill_to_address_extracted value={}", invoice.getBillToAddress());
                }
            }
        }
    }

    private void markFailed(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.FAILED);
        repository.save(invoice);
    }
}
