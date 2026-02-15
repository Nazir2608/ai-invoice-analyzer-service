package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.api.dto.InvoiceUpdateRequest;
import com.nazir.aiinvoice.api.dto.PagedResponse;
import com.nazir.aiinvoice.application.event.InvoiceCreatedEvent;
import com.nazir.aiinvoice.application.mapper.InvoiceMapper;
import com.nazir.aiinvoice.domain.strategy.StorageStrategy;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.model.InvoiceStatus;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageStrategy storageService;

    public InvoiceService(InvoiceRepository repository, ApplicationEventPublisher eventPublisher, StorageStrategy storageService) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.storageService = storageService;
    }

    @Transactional
    public UUID create(InvoiceCreateRequest request) {
        Invoice invoice = InvoiceMapper.toEntity(request);
        repository.save(invoice);
        log.info("Invoice created with id={}", invoice.getId());
        eventPublisher.publishEvent(new InvoiceCreatedEvent(invoice.getId()));
        return invoice.getId();
    }

    public InvoiceResponse get(UUID id) {
        Invoice invoice = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        return InvoiceMapper.toResponse(invoice);
    }

    public PagedResponse<InvoiceResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page,size,Sort.by("createdAt").descending());
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
        log.info("Invoice updated with id={}", id);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Invoice not found");
        }
        repository.deleteById(id);
        log.info("Invoice deleted with id={}", id);
    }

    @Transactional
    public UUID createFromFile(MultipartFile file) {
        // 1. Store file
        String filePath = storageService.store(file);
        // 2. Create invoice entity
        Invoice invoice = Invoice.builder()
                .fileUrl(filePath)
                .status(InvoiceStatus.PENDING)
                .build();
        repository.save(invoice);
        log.info("event=invoice_uploaded invoiceId={}", invoice.getId());
        eventPublisher.publishEvent(new InvoiceCreatedEvent(invoice.getId()));
        return invoice.getId();
    }
}
