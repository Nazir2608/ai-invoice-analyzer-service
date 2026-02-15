package com.nazir.aiinvoice.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private BigDecimal quantity;

    private BigDecimal price;

    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    public InvoiceItem() {
    }

    public InvoiceItem(UUID id, String name, BigDecimal quantity, BigDecimal price, String category, Invoice invoice) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.category = category;
        this.invoice = invoice;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public static InvoiceItemBuilder builder() {
        return new InvoiceItemBuilder();
    }

    public static class InvoiceItemBuilder {
        private UUID id;
        private String name;
        private BigDecimal quantity;
        private BigDecimal price;
        private String category;
        private Invoice invoice;

        InvoiceItemBuilder() {
        }

        public InvoiceItemBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public InvoiceItemBuilder name(String name) {
            this.name = name;
            return this;
        }

        public InvoiceItemBuilder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public InvoiceItemBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public InvoiceItemBuilder category(String category) {
            this.category = category;
            return this;
        }

        public InvoiceItemBuilder invoice(Invoice invoice) {
            this.invoice = invoice;
            return this;
        }

        public InvoiceItem build() {
            return new InvoiceItem(id, name, quantity, price, category, invoice);
        }

        public String toString() {
            return "InvoiceItem.InvoiceItemBuilder(id=" + this.id + ", name=" + this.name + ", quantity=" + this.quantity + ", price=" + this.price + ", category=" + this.category + ", invoice=" + this.invoice + ")";
        }
    }
}
