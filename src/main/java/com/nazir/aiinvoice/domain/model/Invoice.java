package com.nazir.aiinvoice.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends BaseAuditableEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String vendorName;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    private String fileUrl;

    @OneToMany(mappedBy = "invoice",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    // Helper method to maintain bidirectional relationship
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }
}
