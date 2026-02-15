package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.application.event.InvoiceCreatedEvent;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InvoiceProcessingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(InvoiceProcessingOrchestrator.class);
    private final AiExtractionStrategy aiExtractionStrategy;

    public InvoiceProcessingOrchestrator(AiExtractionStrategy aiExtractionStrategy) {
        this.aiExtractionStrategy = aiExtractionStrategy;
    }

    @Async("invoiceExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InvoiceCreatedEvent event) {
        log.info("Starting processing for invoice={}", event.getInvoiceId());
        aiExtractionStrategy.extract(event.getInvoiceId());
    }
}
