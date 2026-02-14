package com.nazir.aiinvoice.application.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class InvoiceCreatedEvent {

    private final UUID invoiceId;
}
