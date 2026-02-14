package com.nazir.aiinvoice.infrastructure.ai;

import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiExtractionService implements AiExtractionStrategy {
    private final InvoiceRepository repository;
    @Override
    public void extract(UUID invoiceId) {
        Invoice invoice = repository.findById(invoiceId).orElseThrow();
        log.info("Mock AI extracting data for invoice={}", invoiceId);
        invoice.setStatus(InvoiceStatus.COMPLETED);
        log.info("Mock AI completed invoice={}", invoiceId);
    }
}
