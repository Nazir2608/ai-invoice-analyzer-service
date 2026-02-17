package com.nazir.aiinvoice.domain.repository;

import com.nazir.aiinvoice.domain.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i")
    BigDecimal sumTotalAmount();

    long countByRiskFlagContaining(String riskFlag);

    long countByPaymentStatus(String paymentStatus);

    @Query("select count(i) from Invoice i where (i.riskFlag is not null and i.riskFlag <> '') or i.status <> com.nazir.aiinvoice.domain.model.InvoiceStatus.COMPLETED")
    long countRequiresReview();
}
