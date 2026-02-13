package com.nazir.aiinvoice.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem extends BaseAuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private BigDecimal quantity;

    private BigDecimal price;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
}
