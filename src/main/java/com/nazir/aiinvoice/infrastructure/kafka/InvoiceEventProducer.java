package com.nazir.aiinvoice.infrastructure.kafka;

import com.nazir.aiinvoice.application.event.InvoiceUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "invoice-uploaded";

    public void publishInvoiceUploaded(UUID invoiceId) {
        InvoiceUploadedEvent event = new InvoiceUploadedEvent(invoiceId);

        kafkaTemplate.send(TOPIC, invoiceId.toString(), event);

        log.info("event=invoice_uploaded_published invoiceId={}", invoiceId);
    }
}
