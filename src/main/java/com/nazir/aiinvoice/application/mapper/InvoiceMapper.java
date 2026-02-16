package com.nazir.aiinvoice.application.mapper;

import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;

public class InvoiceMapper {

    public static Invoice toEntity(InvoiceCreateRequest request) {
        return Invoice.builder()
                .vendorName(request.getVendorName())
                .invoiceNumber(request.getInvoiceNumber())
                .status(InvoiceStatus.PROCESSING)
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
                .extractedRawText(invoice.getExtractedRawText())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
