package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.application.event.InvoiceUploadedEvent;
import com.nazir.aiinvoice.infrastructure.kafka.InvoiceEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.kafka-enabled", havingValue = "true", matchIfMissing = true)
public class InvoiceUploadKafkaPublisher {

    private final InvoiceEventProducer invoiceEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InvoiceUploadedEvent event) {
        log.info("event=invoice_upload_kafka_publish_after_commit invoiceId={}", event.getInvoiceId());
        invoiceEventProducer.publishInvoiceUploaded(event.getInvoiceId());
    }
}
