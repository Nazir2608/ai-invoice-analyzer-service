package com.nazir.aiinvoice.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = true)
    private String vendorName;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    private String fileUrl;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() {
    }

    public Invoice(UUID id, String vendorName, String invoiceNumber, LocalDate invoiceDate, BigDecimal totalAmount, InvoiceStatus status, String fileUrl, List<InvoiceItem> items) {
        this.id = id;
        this.vendorName = vendorName;
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.fileUrl = fileUrl;
        this.items = items != null ? items : new ArrayList<>();
    }

    // Helper method to maintain bidirectional relationship
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    public static InvoiceBuilder builder() {
        return new InvoiceBuilder();
    }

    public static class InvoiceBuilder {
        private UUID id;
        private String vendorName;
        private String invoiceNumber;
        private LocalDate invoiceDate;
        private BigDecimal totalAmount;
        private InvoiceStatus status;
        private String fileUrl;
        private List<InvoiceItem> items;

        InvoiceBuilder() {
        }

        public InvoiceBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public InvoiceBuilder vendorName(String vendorName) {
            this.vendorName = vendorName;
            return this;
        }

        public InvoiceBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public InvoiceBuilder invoiceDate(LocalDate invoiceDate) {
            this.invoiceDate = invoiceDate;
            return this;
        }

        public InvoiceBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public InvoiceBuilder status(InvoiceStatus status) {
            this.status = status;
            return this;
        }

        public InvoiceBuilder fileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
            return this;
        }

        public InvoiceBuilder items(List<InvoiceItem> items) {
            this.items = items;
            return this;
        }

        public Invoice build() {
            return new Invoice(id, vendorName, invoiceNumber, invoiceDate, totalAmount, status, fileUrl, items);
        }

        public String toString() {
            return "Invoice.InvoiceBuilder(id=" + this.id + ", vendorName=" + this.vendorName + ", invoiceNumber=" + this.invoiceNumber + ", invoiceDate=" + this.invoiceDate + ", totalAmount=" + this.totalAmount + ", status=" + this.status + ", fileUrl=" + this.fileUrl + ", items=" + this.items + ")";
        }
    }
}
