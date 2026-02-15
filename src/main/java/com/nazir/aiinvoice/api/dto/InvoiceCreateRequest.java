package com.nazir.aiinvoice.api.dto;

import jakarta.validation.constraints.NotBlank;

public class InvoiceCreateRequest {

    @NotBlank
    private String vendorName;
    @NotBlank
    private String invoiceNumber;

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
}
