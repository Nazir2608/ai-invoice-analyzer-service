package com.nazir.aiinvoice.application.service;

import com.nazir.aiinvoice.domain.model.Invoice;
import com.nazir.aiinvoice.domain.strategy.CategorizationStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "categorization.type", havingValue = "rule", matchIfMissing = true)
public class RuleBasedCategorizationService implements CategorizationStrategy {

    @Override
    public String categorize(Invoice invoice) {
        if (invoice.getVendorName().toLowerCase().contains("amazon")) {
            return "E-COMMERCE";
        }
        return "GENERAL";
    }
}
