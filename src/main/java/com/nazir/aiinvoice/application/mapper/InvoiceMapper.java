package com.nazir.aiinvoice.application.mapper;

import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;

import java.util.UUID;

public class InvoiceMapper {

    public static Invoice toEntity(InvoiceCreateRequest request) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .vendorName(request.getVendorName())
                .invoiceNumber(request.getInvoiceNumber())
                .status(InvoiceStatus.PROCESSING)
                .build();
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .vendorName(invoice.getVendorName())
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
