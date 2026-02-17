package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.api.dto.DashboardResponse;
import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.api.dto.InvoiceUpdateRequest;
import com.nazir.aiinvoice.api.dto.PagedResponse;
import com.nazir.aiinvoice.application.event.InvoiceCreatedEvent;
import com.nazir.aiinvoice.application.mapper.InvoiceMapper;
import com.nazir.aiinvoice.application.service.InvoiceEventService;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceEventType;
import com.nazir.aiinvoice.domain.model.InvoiceRiskConstants;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.domain.strategy.StorageStrategy;
import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageStrategy storageService;
    private final InvoiceEventService invoiceEventService;

    @Transactional
    public UUID create(InvoiceCreateRequest request) {
        Invoice invoice = InvoiceMapper.toEntity(request);
        repository.save(invoice);
        log.info("event=invoice_created invoiceId={}", invoice.getId());
        eventPublisher.publishEvent(new InvoiceCreatedEvent(invoice.getId()));
        return invoice.getId();
    }

    public InvoiceResponse get(UUID id) {
        Invoice invoice = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return InvoiceMapper.toResponse(invoice);
    }

    public PagedResponse<InvoiceResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Invoice> invoicePage = repository.findAll(pageable);
        return PagedResponse.<InvoiceResponse>builder()
                .content(invoicePage.getContent().stream()
                        .map(InvoiceMapper::toResponse)
                        .toList())
                .page(invoicePage.getNumber())
                .size(invoicePage.getSize())
                .totalElements(invoicePage.getTotalElements())
                .totalPages(invoicePage.getTotalPages())
                .build();
    }

    @Transactional
    public void update(UUID id, InvoiceUpdateRequest request) {
        Invoice invoice = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        invoice.setVendorName(request.getVendorName());
        invoice.setInvoiceNumber(request.getInvoiceNumber());
        log.info("event=invoice_updated invoiceId={}", id);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Invoice not found");
        }
        repository.deleteById(id);
        log.info("event=invoice_deleted invoiceId={}", id);
    }

    @Transactional
    public UUID createFromFile(MultipartFile file) {
        String filePath = storageService.store(file);
        Invoice invoice = Invoice.builder()
                .fileUrl(filePath)
                .status(InvoiceStatus.UPLOADED)
                .build();
        repository.save(invoice);
        log.info("event=invoice_uploaded invoiceId={}", invoice.getId());
        eventPublisher.publishEvent(new InvoiceCreatedEvent(invoice.getId()));
        invoiceEventService.record(invoice.getId(), InvoiceEventType.FILE_UPLOADED, "File uploaded and invoice created");
        return invoice.getId();
    }

    public DashboardResponse getDashboard() {
        long totalInvoices = repository.count();
        BigDecimal totalAmount = repository.sumTotalAmount();
        long duplicateCount = repository.countByRiskFlagContaining(InvoiceRiskConstants.RISK_POSSIBLE_DUPLICATE);
        long overdueCount = repository.countByPaymentStatus(InvoiceRiskConstants.PAYMENT_STATUS_OVERDUE);
        long requiresReviewCount = repository.countRequiresReview();

        return DashboardResponse.builder()
                .totalInvoices(totalInvoices)
                .totalAmount(totalAmount)
                .duplicateCount(duplicateCount)
                .overdueCount(overdueCount)
                .requiresReviewCount(requiresReviewCount)
                .build();
    }
}
