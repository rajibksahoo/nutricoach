package com.nutricoach.billing.service;

import com.nutricoach.billing.dto.BillingStatusResponse;
import com.nutricoach.billing.entity.Invoice;
import com.nutricoach.billing.entity.Subscription;
import com.nutricoach.billing.repository.InvoiceRepository;
import com.nutricoach.billing.repository.SubscriptionRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.exception.NutriCoachException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final CoachRepository coachRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final RazorpayService razorpayService;

    private static final int GST_RATE_PERCENT = 18;

    // Prices in paise (INR × 100)
    private static final java.util.Map<Subscription.PlanTier, Integer> PRICES = java.util.Map.of(
            Subscription.PlanTier.STARTER,      99_900,
            Subscription.PlanTier.PROFESSIONAL, 249_900,
            Subscription.PlanTier.ENTERPRISE,   499_900
    );

    @Transactional
    public BillingStatusResponse subscribe(UUID coachId, Subscription.PlanTier tier) {
        Coach coach = requireCoach(coachId);

        // Cancel any existing active subscription first
        subscriptionRepository.findTopByCoachIdOrderByCreatedAtDesc(coachId)
                .filter(s -> s.getStatus() == Subscription.Status.ACTIVE)
                .ifPresent(existing -> {
                    razorpayService.cancelSubscription(existing.getRazorpaySubscriptionId());
                    existing.setStatus(Subscription.Status.CANCELLED);
                    existing.setCancelledAt(Instant.now());
                    subscriptionRepository.save(existing);
                });

        // Create Razorpay customer (idempotent)
        String customerId = razorpayService.ensureCustomer(coach);
        if (coach.getRazorpayCustomerId() == null) {
            coach.setRazorpayCustomerId(customerId);
            coachRepository.save(coach);
        }

        // Create Razorpay subscription
        String[] result = razorpayService.createSubscription(customerId, tier);
        String razorpaySubId = result[0];
        String checkoutUrl   = result[1];

        int amountPaise = PRICES.get(tier);

        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .coachId(coachId)
                .planTier(tier)
                .status(Subscription.Status.TRIAL) // pending payment — becomes ACTIVE on webhook
                .razorpaySubscriptionId(razorpaySubId)
                .amountPaise(amountPaise)
                .build());

        return buildStatus(coach, sub, checkoutUrl);
    }

    @Transactional
    public BillingStatusResponse cancel(UUID coachId) {
        Subscription sub = subscriptionRepository.findTopByCoachIdOrderByCreatedAtDesc(coachId)
                .filter(s -> s.getStatus() == Subscription.Status.ACTIVE)
                .orElseThrow(() -> NutriCoachException.badRequest("No active subscription to cancel"));

        razorpayService.cancelSubscription(sub.getRazorpaySubscriptionId());

        sub.setStatus(Subscription.Status.CANCELLED);
        sub.setCancelledAt(Instant.now());
        subscriptionRepository.save(sub);

        Coach coach = requireCoach(coachId);
        coach.setSubscriptionStatus(Coach.SubscriptionStatus.CANCELLED);
        coachRepository.save(coach);

        return buildStatus(coach, sub, null);
    }

    @Transactional(readOnly = true)
    public BillingStatusResponse getStatus(UUID coachId) {
        Coach coach = requireCoach(coachId);
        Subscription sub = subscriptionRepository.findTopByCoachIdOrderByCreatedAtDesc(coachId).orElse(null);
        return buildStatus(coach, sub, null);
    }

    // ── Webhook handlers ─────────────────────────────────────────────────────

    @Transactional
    public void handleSubscriptionActivated(String razorpaySubId, String planId, Instant periodStart, Instant periodEnd) {
        subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).ifPresent(sub -> {
            sub.setStatus(Subscription.Status.ACTIVE);
            sub.setCurrentPeriodStart(periodStart);
            sub.setCurrentPeriodEnd(periodEnd);
            subscriptionRepository.save(sub);

            coachRepository.findById(sub.getCoachId()).ifPresent(coach -> {
                coach.setSubscriptionTier(toCoachTier(sub.getPlanTier()));
                coach.setSubscriptionStatus(Coach.SubscriptionStatus.ACTIVE);
                coachRepository.save(coach);
                log.info("Coach {} subscription activated: {}", coach.getId(), sub.getPlanTier());
            });
        });
    }

    @Transactional
    public void handlePaymentCaptured(String razorpayPaymentId, String razorpaySubId, int amountPaise) {
        // Idempotency: skip if already processed
        if (invoiceRepository.findByRazorpayPaymentId(razorpayPaymentId).isPresent()) return;

        subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).ifPresent(sub -> {
            int gstPaise = amountPaise * GST_RATE_PERCENT / 100;
            long count = invoiceRepository.countByCoachId(sub.getCoachId()) + 1;
            String invoiceNumber = "NC-" + LocalDate.now().getYear() + "-" + String.format("%04d", count);

            invoiceRepository.save(Invoice.builder()
                    .coachId(sub.getCoachId())
                    .subscriptionId(sub.getId())
                    .razorpayPaymentId(razorpayPaymentId)
                    .invoiceNumber(invoiceNumber)
                    .amountPaise(amountPaise)
                    .gstAmountPaise(gstPaise)
                    .status(Invoice.Status.PAID)
                    .invoiceDate(LocalDate.now())
                    .build());

            log.info("Invoice {} created for coach {} — ₹{}", invoiceNumber, sub.getCoachId(), amountPaise / 100);
        });
    }

    @Transactional
    public void handleSubscriptionCancelled(String razorpaySubId) {
        subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).ifPresent(sub -> {
            sub.setStatus(Subscription.Status.CANCELLED);
            sub.setCancelledAt(Instant.now());
            subscriptionRepository.save(sub);

            coachRepository.findById(sub.getCoachId()).ifPresent(coach -> {
                coach.setSubscriptionStatus(Coach.SubscriptionStatus.CANCELLED);
                coachRepository.save(coach);
            });
        });
    }

    @Transactional
    public void handleSubscriptionHalted(String razorpaySubId) {
        subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).ifPresent(sub -> {
            sub.setStatus(Subscription.Status.PAST_DUE);
            subscriptionRepository.save(sub);

            coachRepository.findById(sub.getCoachId()).ifPresent(coach -> {
                coach.setSubscriptionStatus(Coach.SubscriptionStatus.PAST_DUE);
                coachRepository.save(coach);
            });
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BillingStatusResponse buildStatus(Coach coach, Subscription sub, String checkoutUrl) {
        BillingStatusResponse.ActiveSubscription activeSub = sub == null ? null :
                new BillingStatusResponse.ActiveSubscription(
                        sub.getId(), sub.getPlanTier(), sub.getStatus(),
                        sub.getRazorpaySubscriptionId(), checkoutUrl,
                        sub.getCurrentPeriodStart(), sub.getCurrentPeriodEnd());

        List<BillingStatusResponse.InvoiceSummary> invoices = invoiceRepository
                .findByCoachIdOrderByInvoiceDateDesc(coach.getId())
                .stream()
                .map(inv -> new BillingStatusResponse.InvoiceSummary(
                        inv.getId(), inv.getInvoiceNumber(),
                        inv.getAmountPaise(), inv.getGstAmountPaise(),
                        inv.getStatus(), inv.getInvoiceDate()))
                .toList();

        return new BillingStatusResponse(
                coach.getSubscriptionTier(), coach.getSubscriptionStatus(),
                coach.getTrialEndsAt(), activeSub, invoices);
    }

    private Coach requireCoach(UUID coachId) {
        return coachRepository.findById(coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Coach not found"));
    }

    private Coach.SubscriptionTier toCoachTier(Subscription.PlanTier planTier) {
        return switch (planTier) {
            case STARTER      -> Coach.SubscriptionTier.STARTER;
            case PROFESSIONAL -> Coach.SubscriptionTier.PROFESSIONAL;
            case ENTERPRISE   -> Coach.SubscriptionTier.ENTERPRISE;
        };
    }
}
