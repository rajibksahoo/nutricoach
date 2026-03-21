package com.nutricoach.billing.controller;

import com.nutricoach.billing.dto.BillingStatusResponse;
import com.nutricoach.billing.dto.SubscribeRequest;
import com.nutricoach.billing.service.BillingService;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Billing", description = "Razorpay subscription management")
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingService billingService;
    private final SecurityUtils securityUtils;

    @GetMapping("/status")
    @Operation(summary = "Get billing status", description = "Returns current subscription tier, status, and invoice history")
    public ResponseEntity<ApiResponse<BillingStatusResponse>> getStatus() {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(billingService.getStatus(coachId)));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to a plan", description = "Creates a Razorpay subscription and returns the checkout URL. Cancels any existing active subscription.")
    public ResponseEntity<ApiResponse<BillingStatusResponse>> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        BillingStatusResponse response = billingService.subscribe(coachId, request.planTier());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Subscription created — complete payment at checkoutUrl", response));
    }

    @DeleteMapping("/cancel")
    @Operation(summary = "Cancel subscription", description = "Cancels the active subscription at the end of the current billing period")
    public ResponseEntity<ApiResponse<BillingStatusResponse>> cancel() {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Subscription cancelled", billingService.cancel(coachId)));
    }
}
