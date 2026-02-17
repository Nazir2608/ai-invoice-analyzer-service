package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.domain.model.InvoiceEvent;
import com.nazir.aiinvoice.domain.model.InvoiceEventType;
import com.nazir.aiinvoice.domain.repository.InvoiceEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventService {

    private final InvoiceEventRepository repository;

    public void record(UUID invoiceId, InvoiceEventType type, String message) {
        if (invoiceId == null || type == null) {
            return;
        }
        try {
            InvoiceEvent event = InvoiceEvent.builder()
                    .invoiceId(invoiceId)
                    .eventType(type)
                    .message(message)
                    .build();
            repository.save(event);
        } catch (Exception e) {
            log.warn("event=invoice_event_persist_failed invoiceId={} type={} message={}", invoiceId, type, message, e);
        }
    }
}

