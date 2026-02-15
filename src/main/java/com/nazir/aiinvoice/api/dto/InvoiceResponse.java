package com.nazir.aiinvoice.api.dto;

import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class InvoiceResponse {

    private UUID id;
    private String vendorName;
    private String invoiceNumber;
    private InvoiceStatus status;
    private LocalDateTime createdAt;

    public InvoiceResponse(UUID id, String vendorName, String invoiceNumber, InvoiceStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.vendorName = vendorName;
        this.invoiceNumber = invoiceNumber;
        this.status = status;
        this.createdAt = createdAt;
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

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static InvoiceResponseBuilder builder() {
        return new InvoiceResponseBuilder();
    }

    public static class InvoiceResponseBuilder {
        private UUID id;
        private String vendorName;
        private String invoiceNumber;
        private InvoiceStatus status;
        private LocalDateTime createdAt;

        InvoiceResponseBuilder() {
        }

        public InvoiceResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public InvoiceResponseBuilder vendorName(String vendorName) {
            this.vendorName = vendorName;
            return this;
        }

        public InvoiceResponseBuilder invoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public InvoiceResponseBuilder status(InvoiceStatus status) {
            this.status = status;
            return this;
        }

        public InvoiceResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public InvoiceResponse build() {
            return new InvoiceResponse(id, vendorName, invoiceNumber, status, createdAt);
        }

        public String toString() {
            return "InvoiceResponse.InvoiceResponseBuilder(id=" + this.id + ", vendorName=" + this.vendorName + ", invoiceNumber=" + this.invoiceNumber + ", status=" + this.status + ", createdAt=" + this.createdAt + ")";
        }
    }
}
