package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceEventType;
import com.nazir.aiinvoice.domain.model.InvoiceRiskConstants;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceRiskService {

    private final InvoiceRepository repository;
    private final InvoiceEventService invoiceEventService;

    public void applyRiskChecks(Invoice invoice) {
        applyDuplicateRisk(invoice);
        applyAmountMismatchRisk(invoice);
        applyDueDateRisk(invoice);
        applyAiConfidenceRisk(invoice);
    }

    private void applyDuplicateRisk(Invoice invoice) {
        if (invoice.getId() == null) {
            return;
        }

        String vendorName = invoice.getVendorName();
        String invoiceNumber = invoice.getInvoiceNumber();
        BigDecimal totalAmount = invoice.getTotalAmount();

        if (vendorName == null || invoiceNumber == null) {
            return;
        }

        boolean duplicateByAllFields = false;
        if (totalAmount != null) {
            duplicateByAllFields = repository.existsByVendorNameAndInvoiceNumberAndTotalAmountAndIdNot(
                    vendorName,
                    invoiceNumber,
                    totalAmount,
                    invoice.getId()
            );
        }

        boolean duplicateByBasicFields = repository.existsByVendorNameAndInvoiceNumberAndIdNot(
                vendorName,
                invoiceNumber,
                invoice.getId()
        );

        if (duplicateByAllFields || duplicateByBasicFields) {
            addRiskFlag(invoice, InvoiceRiskConstants.RISK_POSSIBLE_DUPLICATE);
            log.info("event=duplicate_invoice_detected vendorName={} invoiceNumber={} totalAmount={} invoiceId={}",
                    vendorName, invoiceNumber, totalAmount, invoice.getId());
            invoiceEventService.record(invoice.getId(), InvoiceEventType.RISK_FLAG_DUPLICATE, "Possible duplicate detected");
        }
    }

    private void applyAmountMismatchRisk(Invoice invoice) {
        BigDecimal subtotal = invoice.getSubtotal();
        BigDecimal taxAmount = invoice.getTaxAmount();
        BigDecimal totalAmount = invoice.getTotalAmount();

        if (subtotal == null || taxAmount == null || totalAmount == null) {
            return;
        }

        BigDecimal calculated = subtotal.add(taxAmount);
        if (calculated.compareTo(totalAmount) != 0) {
            addRiskFlag(invoice, InvoiceRiskConstants.RISK_AMOUNT_MISMATCH);
            invoiceEventService.record(invoice.getId(), InvoiceEventType.RISK_FLAG_TAX_MISMATCH, "Amount mismatch between subtotal+tax and total");
            markManualReview(invoice, "Tax/amount mismatch between subtotal+tax and total");
        }
    }

    private void applyDueDateRisk(Invoice invoice) {
        if (invoice.getDueDate() == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (invoice.getDueDate().isBefore(today)) {
            invoice.setPaymentStatus(InvoiceRiskConstants.PAYMENT_STATUS_OVERDUE);
        }
    }

    private void applyAiConfidenceRisk(Invoice invoice) {
        Integer confidence = invoice.getAiConfidenceScore();
        if (confidence != null && confidence < 70) {
            markManualReview(invoice, "Low AI confidence: " + confidence);
        }
    }

    private void addRiskFlag(Invoice invoice, String flag) {
        String current = invoice.getRiskFlag();
        if (current == null || current.isBlank()) {
            invoice.setRiskFlag(flag);
        } else if (!current.contains(flag)) {
            invoice.setRiskFlag(current + "," + flag);
        }
    }

    private void markManualReview(Invoice invoice, String reason) {
        invoice.setRequiresManualReview(true);
        String current = invoice.getReviewReason();
        if (current == null || current.isBlank()) {
            invoice.setReviewReason(reason);
        } else if (!current.contains(reason)) {
            invoice.setReviewReason(current + "; " + reason);
        }
        log.info("event=manual_review_flagged invoiceId={} reason={}", invoice.getId(), reason);
    }
}
