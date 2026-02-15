package com.nazir.aiinvoice.infrastructure.ai;

import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiExtractionService implements AiExtractionStrategy {

    private static final Logger log = LoggerFactory.getLogger(MockAiExtractionService.class);

    private final InvoiceRepository repository;

    public MockAiExtractionService(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        log.info("Mock AI extracting data for invoice={}", invoiceId);
        invoice.setStatus(InvoiceStatus.COMPLETED);
        repository.save(invoice);
        log.info("Mock AI completed invoice={}", invoiceId);
    }
}
