package com.nazir.aiinvoice.infrastructure.ai;

import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.AiExtractionStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("hybridExtractionService")
@Slf4j
public class HybridExtractionService implements AiExtractionStrategy {

    private final AiExtractionStrategy localService;
    private final AiExtractionStrategy openAiService;
    private final InvoiceRepository repository;

    public HybridExtractionService(
            @Qualifier("localExtractionService") AiExtractionStrategy localService,
            @Qualifier("openAiExtractionService") AiExtractionStrategy openAiService,
            InvoiceRepository repository) {
        this.localService = localService;
        this.openAiService = openAiService;
        this.repository = repository;
    }

    @Override
    public void extract(UUID invoiceId) {
        log.info("event=hybrid_extraction_started invoiceId={}", invoiceId);
        try {
            localService.extract(invoiceId);
        } catch (Exception e) {
            log.warn("event=local_extraction_exception invoiceId={} message={}", invoiceId, e.getMessage());
        }
        Invoice invoice = repository.findById(invoiceId).orElse(null);
        if (invoice == null) return;
        if (isExtractionInsufficient(invoice)) {
            log.info("event=hybrid_escalation_to_openai invoiceId={} reason=insufficient_data", invoiceId);
            openAiService.extract(invoiceId);
        } else {
            log.info("event=hybrid_extraction_completed_locally invoiceId={}", invoiceId);
        }
    }

    private boolean isExtractionInsufficient(Invoice invoice) {
        if ("FAILED".equals(invoice.getStatus().name())) return true;
        boolean missingTotal = invoice.getTotalAmount() == null;
        boolean missingDate = invoice.getInvoiceDate() == null;
        boolean missingVendor = invoice.getVendorName() == null || invoice.getVendorName().isBlank();
        boolean missingInvoiceNumber = invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank();
        int missingCount = 0;
        if (missingTotal) missingCount++;
        if (missingDate) missingCount++;
        if (missingVendor) missingCount++;
        if (missingInvoiceNumber) missingCount++;
        return missingCount >= 2;
    }
}
