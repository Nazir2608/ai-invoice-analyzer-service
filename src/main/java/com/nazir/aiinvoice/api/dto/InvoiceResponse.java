package com.nazir.aiinvoice.api.dto;

import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InvoiceResponse {

    private UUID id;
    private String vendorName;
    private String invoiceNumber;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
}
