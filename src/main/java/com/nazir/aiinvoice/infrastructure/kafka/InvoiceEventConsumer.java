package com.nazir.aiinvoice.infrastructure.kafka;

import com.nazir.aiinvoice.application.event.InvoiceUploadedEvent;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.kafka-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventConsumer {

    private final InvoiceRepository invoiceRepository;
    private final AiExtractionStrategy extractionStrategy;
    private final MeterRegistry meterRegistry;
    private Counter invoiceConsumedCounter;

    @PostConstruct
    void initMetrics() {
        invoiceConsumedCounter = Counter.builder("invoice.upload.consumed.total")
                .description("Total invoices consumed from Kafka")
                .register(meterRegistry);
    }

    @KafkaListener(topics = "invoice-uploaded", groupId = "invoice-group")
    public void consume(InvoiceUploadedEvent event) {
        log.info("event=invoice_uploaded_consumed invoiceId={}", event.getInvoiceId());
        if (invoiceConsumedCounter != null) {
            invoiceConsumedCounter.increment();
        }
        Invoice invoice = invoiceRepository.findById(event.getInvoiceId()).orElse(null);
        if (invoice == null) {
            log.warn("event=invoice_not_found_for_message invoiceId={}", event.getInvoiceId());
            return;
        }
        if (InvoiceStatus.COMPLETED.equals(invoice.getStatus())) {
            log.info("event=duplicate_message_skipped extraction invoiceId={}", event.getInvoiceId());
            return;
        }
        extractionStrategy.extract(event.getInvoiceId());
    }
}
