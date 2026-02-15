package com.nazir.aiinvoice.domain.strategy;

import com.nazir.aiinvoice.domain.model.Invoice;

public interface CategorizationStrategy {

        String categorize(Invoice invoice);
}
