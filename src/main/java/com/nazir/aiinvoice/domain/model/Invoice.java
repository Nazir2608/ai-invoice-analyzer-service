package com.nazir.aiinvoice.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue
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
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String extractedRawText;
    private LocalDateTime createdAt;
}
