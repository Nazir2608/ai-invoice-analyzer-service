package com.nazir.aiinvoice.application.event;

import java.util.UUID;

public class InvoiceCreatedEvent {
    private final UUID invoiceId;
    public InvoiceCreatedEvent(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }
    public UUID getInvoiceId() {
        return invoiceId;
    }
}
