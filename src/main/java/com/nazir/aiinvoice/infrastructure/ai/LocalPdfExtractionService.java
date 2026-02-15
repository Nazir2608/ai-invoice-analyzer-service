package com.nazir.aiinvoice.infrastructure.ai;

import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ai.provider", havingValue = "local")
public class LocalPdfExtractionService implements AiExtractionStrategy {

    private final InvoiceRepository repository;

    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("(?i)invoice\\s*(?:no|number|#)?\\s*[:.]?\\s*([A-Z0-9\\-]+)");

    private static final Pattern TOTAL_PATTERN = Pattern.compile("(?i)(total|amount\\s*due|balance\\s*due)\\s*[:.]?\\s*[$€£]?\\s*([\\d,]+\\.?\\d{0,2})");

    private static final Pattern DATE_PATTERN = Pattern.compile("(?i)(?:invoice\\s*)?date\\s*[:.]?\\s*(\\d{4}-\\d{2}-\\d{2})");

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        log.info("event=invoice_extraction_started invoiceId={}", invoiceId);
        try {
            String text = extractText(invoice.getFileUrl());
            if (text == null || text.isBlank()) {
                log.warn("event=empty_text invoiceId={}", invoiceId);
                markFailed(invoice);
                return;
            }
            parseAndPopulate(invoice, text);
            invoice.setStatus(InvoiceStatus.COMPLETED);
            repository.save(invoice);
            log.info("event=invoice_extraction_completed invoiceId={} vendor={} total={}", invoiceId, invoice.getVendorName(), invoice.getTotalAmount());
        } catch (Exception ex) {
            log.error("event=invoice_extraction_failed invoiceId={}", invoiceId, ex);
            markFailed(invoice);
        }
    }

    private String extractText(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ResourceNotFoundException("File not found: " + filePath);
        }
        if (filePath.toLowerCase().endsWith(".txt")) {
            log.info("event=txt_extraction path={}", filePath);
            return Files.readString(file.toPath());
        }
        if (!filePath.toLowerCase().endsWith(".pdf")) {
            log.warn("event=unsupported_file_type path={}", filePath);
            return null;
        }
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("event=pdf_extracted length={}", text.length());
            log.debug("extracted_text=\n{}", text);
            return text;
        }
    }

    private void parseAndPopulate(Invoice invoice, String text) {
        extractInvoiceNumber(invoice, text);
        extractTotal(invoice, text);
        extractDate(invoice, text);
        extractVendor(invoice, text);
    }

    private void extractInvoiceNumber(Invoice invoice, String text) {
        Matcher matcher = INVOICE_NUMBER_PATTERN.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1);
            if (!value.equalsIgnoreCase("date")) {
                invoice.setInvoiceNumber(value);
                log.info("event=invoice_number_extracted value={}", value);
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

    private void extractDate(Invoice invoice, String text) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                LocalDate date = LocalDate.parse(matcher.group(1));
                invoice.setInvoiceDate(date);
                log.info("event=date_extracted value={}", date);
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
            invoice.setVendorName(trimmed);
            log.info("event=vendor_extracted value={}", trimmed);
            break;
        }
    }

    private void markFailed(Invoice invoice) {
        invoice.setStatus(InvoiceStatus.FAILED);
        repository.save(invoice);
    }
}
