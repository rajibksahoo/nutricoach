package com.nutricoach.billing.repository;

import com.nutricoach.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByCoachIdOrderByInvoiceDateDesc(UUID coachId);

    Optional<Invoice> findByRazorpayPaymentId(String razorpayPaymentId);

    long countByCoachId(UUID coachId);
}
