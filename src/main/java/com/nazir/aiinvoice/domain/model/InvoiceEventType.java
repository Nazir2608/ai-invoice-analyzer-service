package com.nazir.aiinvoice.domain.model;

public enum InvoiceEventType {
    FILE_UPLOADED,
    TEXT_EXTRACTED,
    AI_STARTED,
    AI_COMPLETED,
    RISK_FLAG_DUPLICATE,
    RISK_FLAG_TAX_MISMATCH,
    PROCESSING_FAILED
}

