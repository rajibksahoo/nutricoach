package com.nutricoach.plans.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.plans.dto.MealPlanResponse;
import com.nutricoach.plans.dto.MealPlanSummaryResponse;
import com.nutricoach.plans.service.MealPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portal/meal-plans")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Client Portal — Meal Plans", description = "Read-only meal plan access for clients")
public class ClientMealPlanController {

    private final MealPlanService mealPlanService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List my meal plans", description = "Returns all meal plans assigned to the authenticated client")
    public ResponseEntity<ApiResponse<List<MealPlanSummaryResponse>>> list() {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(mealPlanService.listPlans(clientId, coachId)));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get meal plan detail", description = "Returns the full meal plan with all days, meals, and food items")
    public ResponseEntity<ApiResponse<MealPlanResponse>> get(@PathVariable UUID planId) {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(mealPlanService.getFullPlanForClient(planId, clientId, coachId)));
    }
}
