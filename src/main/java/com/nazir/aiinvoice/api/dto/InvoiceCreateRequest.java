package com.nazir.aiinvoice.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceCreateRequest {

    @NotBlank
    private String vendorName;

    private String invoiceNumber;
}
