package com.nazir.aiinvoice.domain.strategy;

import java.util.UUID;

public interface AiExtractionStrategy {
    void extract(UUID invoiceId);
}
