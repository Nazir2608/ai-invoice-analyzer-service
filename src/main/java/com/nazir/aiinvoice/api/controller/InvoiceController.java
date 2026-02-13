package com.nazir.aiinvoice.api.controller;

import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.application.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @PostMapping
    public UUID create(@Valid @RequestBody InvoiceCreateRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public Page<InvoiceResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.list(page, size);
    }
}
