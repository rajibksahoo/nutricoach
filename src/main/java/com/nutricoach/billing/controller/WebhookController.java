package com.nutricoach.billing.controller;

import com.nutricoach.billing.service.BillingService;
import com.nutricoach.billing.service.RazorpayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing/webhook")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "Razorpay subscription management")
public class WebhookController {

    private final BillingService billingService;
    private final RazorpayService razorpayService;

    @PostMapping
    @Operation(summary = "Razorpay webhook receiver", description = "Handles subscription lifecycle events from Razorpay. No auth required — verified via HMAC-SHA256 signature.")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("Razorpay webhook signature mismatch — rejecting");
            return ResponseEntity.status(400).build();
        }

        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.getString("event");
            JSONObject entity = event.getJSONObject("payload")
                    .getJSONObject("subscription")
                    .getJSONObject("entity");

            log.info("Razorpay webhook: {}", eventType);

            switch (eventType) {
                case "subscription.activated" -> {
                    String subId        = entity.getString("id");
                    Instant periodStart = Instant.ofEpochSecond(entity.optLong("current_start", 0));
                    Instant periodEnd   = Instant.ofEpochSecond(entity.optLong("current_end", 0));
                    billingService.handleSubscriptionActivated(subId, entity.optString("plan_id"), periodStart, periodEnd);
                }
                case "subscription.charged" -> {
                    String subId = entity.getString("id");
                    JSONObject payment = event.getJSONObject("payload")
                            .getJSONObject("payment")
                            .getJSONObject("entity");
                    billingService.handlePaymentCaptured(payment.getString("id"), subId, payment.getInt("amount"));
                }
                case "subscription.cancelled" ->
                        billingService.handleSubscriptionCancelled(entity.getString("id"));

                case "subscription.halted" ->
                        billingService.handleSubscriptionHalted(entity.getString("id"));

                default -> log.debug("Unhandled Razorpay event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(500).build();
        }

        return ResponseEntity.ok().build();
    }
}
