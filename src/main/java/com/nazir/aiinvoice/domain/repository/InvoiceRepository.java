package com.nazir.aiinvoice.domain.repository;

import com.nazir.aiinvoice.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    boolean existsByVendorNameAndInvoiceNumberAndTotalAmountAndIdNot(
            String vendorName,
            String invoiceNumber,
            BigDecimal totalAmount,
            UUID id
    );

    boolean existsByVendorNameAndInvoiceNumberAndIdNot(
            String vendorName,
            String invoiceNumber,
            UUID id
    );
}
