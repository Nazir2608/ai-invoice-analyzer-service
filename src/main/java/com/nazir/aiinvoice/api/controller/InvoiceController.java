package com.nazir.aiinvoice.api.controller;

import com.nazir.aiinvoice.api.dto.*;
import com.nazir.aiinvoice.application.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final Logger log = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<UUID> create(@Valid @RequestBody InvoiceCreateRequest request) {
        log.info("creating invoice: {}", request);
        UUID id = service.create(request);
        return ApiResponse.<UUID>builder()
                .success(true)
                .data(id)
                .message("Invoice created successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public ApiResponse<PagedResponse<InvoiceResponse>> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        PagedResponse<InvoiceResponse> result = service.list(page, size);
        return ApiResponse.<PagedResponse<InvoiceResponse>>builder()
                .success(true)
                .data(result)
                .message("Invoices fetched successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<String> update(@PathVariable UUID id, @RequestBody InvoiceUpdateRequest request) {
        service.update(id, request);
        return ApiResponse.<String>builder()
                .success(true)
                .data("Updated")
                .message("Invoice updated successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable UUID id) {
        service.delete(id);
        return ApiResponse.<String>builder()
                .success(true)
                .data("Deleted")
                .message("Invoice deleted successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ApiResponse<UUID> upload(@RequestParam("file") MultipartFile file) {
        log.info("Upload endpoint hit. File name={}", file.getOriginalFilename());
        log.info("uploading invoice..");
        UUID id = service.createFromFile(file);
        return ApiResponse.<UUID>builder()
                .success(true)
                .data(id)
                .message("Invoice uploaded successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

}
