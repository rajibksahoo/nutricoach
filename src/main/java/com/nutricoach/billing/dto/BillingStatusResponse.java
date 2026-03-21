package com.nutricoach.billing.dto;

import com.nutricoach.billing.entity.Invoice;
import com.nutricoach.billing.entity.Subscription;
import com.nutricoach.coach.entity.Coach;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BillingStatusResponse(
        Coach.SubscriptionTier tier,
        Coach.SubscriptionStatus status,
        Instant trialEndsAt,
        ActiveSubscription activeSubscription,
        List<InvoiceSummary> invoices
) {
    public record ActiveSubscription(
            UUID id,
            Subscription.PlanTier planTier,
            Subscription.Status subscriptionStatus,
            String razorpaySubscriptionId,
            String checkoutUrl,
            Instant currentPeriodStart,
            Instant currentPeriodEnd
    ) {}

    public record InvoiceSummary(
            UUID id,
            String invoiceNumber,
            int amountPaise,
            int gstAmountPaise,
            Invoice.Status status,
            LocalDate invoiceDate
    ) {}
}
