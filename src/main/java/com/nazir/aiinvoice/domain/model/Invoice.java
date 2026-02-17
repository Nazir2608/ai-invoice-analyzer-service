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
@Table(name = "invoice")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends BaseAuditableEntity {

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
    private String riskFlag;
    private String paymentStatus;
    private Integer aiConfidenceScore;
    private Boolean requiresManualReview;
    @Column(columnDefinition = "TEXT")
    private String reviewReason;
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String extractedRawText;
    @Column(columnDefinition = "TEXT")
    private String aiSummary;
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();
}
