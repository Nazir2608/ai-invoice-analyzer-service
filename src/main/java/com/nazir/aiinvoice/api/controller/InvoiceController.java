package com.nazir.aiinvoice.api.controller;

import com.nazir.aiinvoice.api.dto.ApiResponse;
import com.nazir.aiinvoice.api.dto.DashboardResponse;
import com.nazir.aiinvoice.api.dto.InvoiceCreateRequest;
import com.nazir.aiinvoice.api.dto.InvoiceResponse;
import com.nazir.aiinvoice.api.dto.InvoiceUpdateRequest;
import com.nazir.aiinvoice.api.dto.PagedResponse;
import com.nazir.aiinvoice.application.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService service;

    @PostMapping
    public ApiResponse<UUID> create(@Valid @RequestBody InvoiceCreateRequest request) {
        log.info("event=invoice_create_request vendorName={} invoiceNumber={}", request.getVendorName(), request.getInvoiceNumber());
        UUID id = service.create(request);
        return ApiResponse.<UUID>builder()
                .success(true)
                .data(id)
                .message("Invoice created successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceResponse> get(@PathVariable UUID id) {
        InvoiceResponse response = service.get(id);
        return ApiResponse.<InvoiceResponse>builder()
                .success(true)
                .data(response)
                .message("Invoice fetched successfully")
                .timestamp(LocalDateTime.now())
                .build();
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

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() {
        DashboardResponse response = service.getDashboard();
        return ApiResponse.<DashboardResponse>builder()
                .success(true)
                .data(response)
                .message("Dashboard data fetched successfully")
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
        log.info("event=invoice_upload_request fileName={} size={}", file.getOriginalFilename(), file.getSize());
        UUID id = service.createFromFile(file);
        return ApiResponse.<UUID>builder()
                .success(true)
                .data(id)
                .message("Invoice uploaded successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
