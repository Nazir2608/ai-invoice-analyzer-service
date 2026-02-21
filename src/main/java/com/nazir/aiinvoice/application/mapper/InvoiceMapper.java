package com.nazir.aiinvoice.application.mapper;

import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceLineItemResponse;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceItem;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class InvoiceMapper {

    public static Invoice toEntity(InvoiceCreateRequest request) {
        return Invoice.builder()
                .vendorName(request.getVendorName())
                .invoiceNumber(request.getInvoiceNumber())
                .status(InvoiceStatus.UPLOADED)
                .build();
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .vendorName(invoice.getVendorName())
                .vendorEmail(invoice.getVendorEmail())
                .vendorAddress(invoice.getVendorAddress())
                .billToName(invoice.getBillToName())
                .billToAddress(invoice.getBillToAddress())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .riskFlag(invoice.getRiskFlag())
                .paymentStatus(invoice.getPaymentStatus())
                .aiSummary(invoice.getAiSummary())
                .aiConfidenceScore(invoice.getAiConfidenceScore())
                .requiresManualReview(invoice.getRequiresManualReview())
                .reviewReason(invoice.getReviewReason())
                .lineItems(toLineItemResponses(invoice.getItems()))
                .extractedRawText(invoice.getExtractedRawText())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private static List<InvoiceLineItemResponse> toLineItemResponses(List<InvoiceItem> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(InvoiceMapper::toLineItemResponse)
                .collect(Collectors.toList());
    }

    private static InvoiceLineItemResponse toLineItemResponse(InvoiceItem item) {
        BigDecimal quantity = item.getQuantity();
        BigDecimal unitPrice = item.getPrice();
        BigDecimal lineTotal = item.getLineTotal();
        if (lineTotal == null && quantity != null && unitPrice != null) {
            lineTotal = unitPrice.multiply(quantity);
        }
        return InvoiceLineItemResponse.builder()
                .description(item.getName())
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lineTotal(lineTotal)
                .build();
    }
}
