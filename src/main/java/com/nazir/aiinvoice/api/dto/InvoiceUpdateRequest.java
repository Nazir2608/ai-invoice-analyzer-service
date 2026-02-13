package com.nazir.aiinvoice.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceUpdateRequest {

    private String vendorName;
    private String invoiceNumber;
}
