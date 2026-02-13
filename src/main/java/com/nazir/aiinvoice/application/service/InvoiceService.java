package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.application.mapper.InvoiceMapper;
import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.repository.InvoiceRepository;
import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository repository;

    public UUID create(InvoiceCreateRequest request) {

        Invoice invoice = InvoiceMapper.toEntity(request);

        repository.save(invoice);

        log.info("Invoice created with id={}", invoice.getId());

        return invoice.getId();
    }

    public InvoiceResponse get(UUID id) {

        Invoice invoice = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        return InvoiceMapper.toResponse(invoice);
    }

    public Page<InvoiceResponse> list(int page, int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        return repository.findAll(pageable)
                .map(InvoiceMapper::toResponse);
    }
}
