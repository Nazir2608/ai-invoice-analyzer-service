package com.nazir.aiinvoice.api.dto;

import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class InvoiceResponse {

    private UUID id;
    private String vendorName;
    private String vendorEmail;
    private String vendorAddress;
    
    private String billToName;
    private String billToAddress;
    
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    
    private InvoiceStatus status;
    private String extractedRawText;
    private LocalDateTime createdAt;
}
