package com.nutricoach.billing.service;

import com.nutricoach.billing.config.RazorpayProperties;
import com.nutricoach.billing.entity.Subscription;
import com.nutricoach.coach.entity.Coach;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayClient razorpayClient;
    private final RazorpayProperties props;

    /**
     * Creates or returns the Razorpay customer ID for a coach.
     * In dev mode returns a dummy ID.
     */
    public String ensureCustomer(Coach coach) {
        if (props.isDevMode()) {
            return "cust_dev_" + coach.getId().toString().replace("-", "").substring(0, 14);
        }
        if (coach.getRazorpayCustomerId() != null) {
            return coach.getRazorpayCustomerId();
        }
        try {
            JSONObject req = new JSONObject();
            req.put("name", coach.getName());
            req.put("contact", coach.getPhone());
            if (coach.getEmail() != null) req.put("email", coach.getEmail());
            com.razorpay.Customer customer = razorpayClient.customers.create(req);
            return customer.get("id");
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay customer for coach {}", coach.getId(), e);
            throw new RuntimeException("Failed to create payment customer: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a Razorpay subscription for the given tier.
     * Returns [razorpaySubscriptionId, checkoutUrl].
     * In dev mode returns dummy values.
     */
    public String[] createSubscription(String razorpayCustomerId, Subscription.PlanTier tier) {
        if (props.isDevMode()) {
            String subId = "sub_dev_" + tier.name().toLowerCase();
            String url = "https://rzp.io/dev/" + subId;
            log.info("[DEV] Razorpay subscription created: {} → {}", subId, url);
            return new String[]{subId, url};
        }
        try {
            String planId = props.planIdFor(tier.name());
            JSONObject req = new JSONObject();
            req.put("plan_id", planId);
            req.put("customer_id", razorpayCustomerId);
            req.put("customer_notify", 1);
            req.put("quantity", 1);
            req.put("total_count", 120); // up to 10 years, cancel anytime
            com.razorpay.Subscription sub = razorpayClient.subscriptions.create(req);
            return new String[]{sub.get("id"), sub.get("short_url")};
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay subscription", e);
            throw new RuntimeException("Failed to create subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Cancels a Razorpay subscription at period end.
     * In dev mode is a no-op.
     */
    public void cancelSubscription(String razorpaySubscriptionId) {
        if (props.isDevMode()) {
            log.info("[DEV] Razorpay subscription cancelled: {}", razorpaySubscriptionId);
            return;
        }
        try {
            JSONObject req = new JSONObject();
            req.put("cancel_at_cycle_end", 1); // cancel gracefully at period end
            razorpayClient.subscriptions.cancel(razorpaySubscriptionId, req);
        } catch (RazorpayException e) {
            log.error("Failed to cancel Razorpay subscription {}", razorpaySubscriptionId, e);
            throw new RuntimeException("Failed to cancel subscription: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies a Razorpay webhook signature.
     * In dev mode always returns true.
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (props.isDevMode()) {
            return true;
        }
        try {
            Utils.verifyWebhookSignature(payload, signature, props.getWebhookSecret());
            return true;
        } catch (RazorpayException e) {
            log.warn("Razorpay webhook signature verification failed", e);
            return false;
        }
    }
}
