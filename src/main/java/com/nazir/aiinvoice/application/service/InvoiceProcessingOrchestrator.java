package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.application.event.InvoiceCreatedEvent;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceProcessingOrchestrator {

    private final AiExtractionStrategy aiExtractionStrategy;

    @Async("invoiceExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InvoiceCreatedEvent event) {
        log.info("event=invoice_processing_started invoiceId={}", event.getInvoiceId());
        aiExtractionStrategy.extract(event.getInvoiceId());
    }
}
