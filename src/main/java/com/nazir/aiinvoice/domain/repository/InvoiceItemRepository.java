package com.nazir.aiinvoice.domain.repository;

import com.nazir.aiinvoice.domain.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {
}
