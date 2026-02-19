package com.nazir.aiinvoice.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "invoice_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceEvent extends BaseAuditableEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID invoiceId;
    @Enumerated(EnumType.STRING)
    private InvoiceEventType eventType;
    private String message;
}

