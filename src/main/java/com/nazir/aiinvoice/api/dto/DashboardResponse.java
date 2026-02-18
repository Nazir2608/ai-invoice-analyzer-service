package com.nazir.aiinvoice.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
public class DashboardResponse {

    private long totalInvoices;
    private BigDecimal totalAmount;
    private long duplicateCount;
    private long overdueCount;
    private long requiresReviewCount;
}
