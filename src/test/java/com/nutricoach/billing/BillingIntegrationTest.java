package com.nutricoach.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nutricoach.AbstractIntegrationTest;
import com.nutricoach.billing.entity.Subscription;
import com.nutricoach.billing.repository.InvoiceRepository;
import com.nutricoach.billing.repository.SubscriptionRepository;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.security.JwtService;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CoachRepository coachRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired InvoiceRepository invoiceRepository;
    @Autowired JwtService jwtService;

    // Dev mode is true in application-test.yml — RazorpayClient bean won't make real calls.
    // We still declare MockBean so Spring doesn't fail if the bean needs overriding.
    @MockBean RazorpayClient razorpayClient;

    private String jwt;
    private Coach coach;

    @BeforeEach
    void setup() {
        coachRepository.findByPhone("9200000001").ifPresent(existing -> {
            invoiceRepository.deleteAll(invoiceRepository.findByCoachIdOrderByInvoiceDateDesc(existing.getId()));
            subscriptionRepository.deleteAll(subscriptionRepository.findByCoachIdOrderByCreatedAtDesc(existing.getId()));
            clientRepository.deleteAll(clientRepository.findAllByCoachId(existing.getId()));
            coachRepository.delete(existing);
        });

        coach = coachRepository.save(Coach.builder()
                .phone("9200000001")
                .name("Billing Coach")
                .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L))
                .build());

        jwt = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
    }

    // ── Status ────────────────────────────────────────────────────────────────

    @Test
    void getStatus_newCoach_returnsTrial() throws Exception {
        mockMvc.perform(get("/api/v1/billing/status")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tier").value("STARTER"))
                .andExpect(jsonPath("$.data.status").value("TRIAL"))
                .andExpect(jsonPath("$.data.activeSubscription").doesNotExist())
                .andExpect(jsonPath("$.data.invoices").isArray());
    }

    @Test
    void getStatus_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/billing/status"))
                .andExpect(status().isUnauthorized());
    }

    // ── Subscribe ─────────────────────────────────────────────────────────────

    @Test
    void subscribe_starterPlan_returns201WithCheckoutUrl() throws Exception {
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "STARTER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeSubscription.planTier").value("STARTER"))
                .andExpect(jsonPath("$.data.activeSubscription.razorpaySubscriptionId").exists())
                .andExpect(jsonPath("$.data.activeSubscription.checkoutUrl").exists());
    }

    @Test
    void subscribe_professionalPlan_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "PROFESSIONAL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activeSubscription.planTier").value("PROFESSIONAL"));
    }

    @Test
    void subscribe_missingPlanTier_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancel_noActiveSubscription_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/billing/cancel")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancel_afterSubscribe_cancels() throws Exception {
        // Subscribe first
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "STARTER"))))
                .andExpect(status().isCreated());

        // Manually activate the subscription (normally done via webhook)
        subscriptionRepository.findTopByCoachIdOrderByCreatedAtDesc(coach.getId()).ifPresent(sub -> {
            sub.setStatus(Subscription.Status.ACTIVE);
            subscriptionRepository.save(sub);
        });

        mockMvc.perform(delete("/api/v1/billing/cancel")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeSubscription.subscriptionStatus").value("CANCELLED"));
    }

    // ── Webhook ───────────────────────────────────────────────────────────────

    @Test
    void webhook_subscriptionActivated_updatesCoachStatus() throws Exception {
        // Subscribe to get a razorpaySubscriptionId
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "PROFESSIONAL"))))
                .andExpect(status().isCreated());

        String razorpaySubId = subscriptionRepository
                .findTopByCoachIdOrderByCreatedAtDesc(coach.getId())
                .map(Subscription::getRazorpaySubscriptionId)
                .orElseThrow();

        String webhookPayload = """
                {
                  "event": "subscription.activated",
                  "payload": {
                    "subscription": {
                      "entity": {
                        "id": "%s",
                        "plan_id": "plan_test_professional",
                        "current_start": 1700000000,
                        "current_end": 1702592000
                      }
                    }
                  }
                }
                """.formatted(razorpaySubId);

        // dev-mode: no signature check
        mockMvc.perform(post("/api/v1/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        // Coach status should now be ACTIVE
        mockMvc.perform(get("/api/v1/billing/status")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.tier").value("PROFESSIONAL"));
    }

    @Test
    void webhook_paymentCaptured_createsInvoice() throws Exception {
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "STARTER"))))
                .andExpect(status().isCreated());

        String razorpaySubId = subscriptionRepository
                .findTopByCoachIdOrderByCreatedAtDesc(coach.getId())
                .map(Subscription::getRazorpaySubscriptionId)
                .orElseThrow();

        String webhookPayload = """
                {
                  "event": "subscription.charged",
                  "payload": {
                    "subscription": {
                      "entity": { "id": "%s" }
                    },
                    "payment": {
                      "entity": {
                        "id": "pay_test_abc123",
                        "amount": 99900
                      }
                    }
                  }
                }
                """.formatted(razorpaySubId);

        mockMvc.perform(post("/api/v1/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        // Invoice should appear in status
        mockMvc.perform(get("/api/v1/billing/status")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoices.length()").value(1))
                .andExpect(jsonPath("$.data.invoices[0].amountPaise").value(99900))
                .andExpect(jsonPath("$.data.invoices[0].status").value("PAID"));
    }

    @Test
    void webhook_paymentCaptured_idempotent() throws Exception {
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "STARTER"))))
                .andExpect(status().isCreated());

        String razorpaySubId = subscriptionRepository
                .findTopByCoachIdOrderByCreatedAtDesc(coach.getId())
                .map(Subscription::getRazorpaySubscriptionId)
                .orElseThrow();

        String webhookPayload = """
                {
                  "event": "subscription.charged",
                  "payload": {
                    "subscription": { "entity": { "id": "%s" } },
                    "payment": { "entity": { "id": "pay_dupe_xyz", "amount": 99900 } }
                  }
                }
                """.formatted(razorpaySubId);

        // Send same webhook twice
        mockMvc.perform(post("/api/v1/billing/webhook").contentType(MediaType.APPLICATION_JSON).content(webhookPayload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/billing/webhook").contentType(MediaType.APPLICATION_JSON).content(webhookPayload))
                .andExpect(status().isOk());

        // Only 1 invoice created
        mockMvc.perform(get("/api/v1/billing/status").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoices.length()").value(1));
    }

    // ── Feature gating ────────────────────────────────────────────────────────

    @Test
    void createClient_overTrialLimit_returns402() throws Exception {
        // Trial = 5 clients max. Create 5 first.
        for (int i = 0; i < 5; i++) {
            clientRepository.save(Client.builder()
                    .coachId(coach.getId())
                    .phone("980000" + String.format("%04d", i))
                    .name("Client " + i)
                    .status(Client.Status.ACTIVE)
                    .build());
        }

        // 6th client should be blocked
        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "9899999999",
                                "name", "Over Limit Client"
                        ))))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createClient_activeSubscription_allowsMoreClients() throws Exception {
        // Activate subscription (STARTER = 25 clients)
        subscriptionRepository.save(Subscription.builder()
                .coachId(coach.getId())
                .planTier(Subscription.PlanTier.STARTER)
                .status(Subscription.Status.ACTIVE)
                .amountPaise(99_900)
                .razorpaySubscriptionId("sub_active_test")
                .build());

        coach.setSubscriptionStatus(Coach.SubscriptionStatus.ACTIVE);
        coach.setSubscriptionTier(Coach.SubscriptionTier.STARTER);
        coachRepository.save(coach);

        // Re-generate JWT to reflect updated coach (JWT still works as coachId is the same)
        // Create 5 clients — should succeed even past trial limit
        for (int i = 0; i < 5; i++) {
            clientRepository.save(Client.builder()
                    .coachId(coach.getId())
                    .phone("970000" + String.format("%04d", i))
                    .name("Client " + i)
                    .status(Client.Status.ACTIVE)
                    .build());
        }

        // 6th via API should be allowed (STARTER = 25)
        mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "9710000099",
                                "name", "Allowed Client"
                        ))))
                .andExpect(status().isCreated());
    }

    @Test
    void webhook_subscriptionCancelled_updatesCoachStatus() throws Exception {
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "STARTER"))))
                .andExpect(status().isCreated());

        String razorpaySubId = subscriptionRepository
                .findTopByCoachIdOrderByCreatedAtDesc(coach.getId())
                .map(Subscription::getRazorpaySubscriptionId)
                .orElseThrow();

        String webhookPayload = """
                {
                  "event": "subscription.cancelled",
                  "payload": {
                    "subscription": {
                      "entity": { "id": "%s" }
                    }
                  }
                }
                """.formatted(razorpaySubId);

        mockMvc.perform(post("/api/v1/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/billing/status")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void webhook_subscriptionHalted_updatesCoachStatusToPastDue() throws Exception {
        mockMvc.perform(post("/api/v1/billing/subscribe")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("planTier", "PROFESSIONAL"))))
                .andExpect(status().isCreated());

        String razorpaySubId = subscriptionRepository
                .findTopByCoachIdOrderByCreatedAtDesc(coach.getId())
                .map(Subscription::getRazorpaySubscriptionId)
                .orElseThrow();

        String webhookPayload = """
                {
                  "event": "subscription.halted",
                  "payload": {
                    "subscription": {
                      "entity": { "id": "%s" }
                    }
                  }
                }
                """.formatted(razorpaySubId);

        mockMvc.perform(post("/api/v1/billing/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/billing/status")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAST_DUE"));
    }
}
