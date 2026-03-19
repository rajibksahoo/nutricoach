package com.nutricoach.billing.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(name = "razorpay_payment_id", length = 50)
    private String razorpayPaymentId;

    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;

    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;

    @Builder.Default
    @Column(name = "gst_amount_paise", nullable = false)
    private int gstAmountPaise = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    public enum Status { PAID, PENDING, FAILED, REFUNDED }
}
