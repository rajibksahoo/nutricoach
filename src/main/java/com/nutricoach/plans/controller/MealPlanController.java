package com.nutricoach.plans.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.plans.dto.*;
import com.nutricoach.plans.entity.MealPlan;
import com.nutricoach.plans.service.MealPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Meal Plans", description = "Meal plan builder — plans, days, meals, and food items")
public class MealPlanController {

    private final MealPlanService mealPlanService;
    private final SecurityUtils securityUtils;

    // ── Meal Plans ────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/clients/{clientId}/meal-plans")
    @Operation(summary = "Create meal plan for a client")
    public ResponseEntity<ApiResponse<MealPlanSummaryResponse>> create(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateMealPlanRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        MealPlanSummaryResponse plan = mealPlanService.createPlan(clientId, coachId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Meal plan created", plan));
    }

    @GetMapping("/api/v1/clients/{clientId}/meal-plans")
    @Operation(summary = "List all meal plans for a client")
    public ResponseEntity<ApiResponse<List<MealPlanSummaryResponse>>> list(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(mealPlanService.listPlans(clientId, coachId)));
    }

    @GetMapping("/api/v1/meal-plans/{planId}")
    @Operation(summary = "Get full meal plan with all days, meals, and food items")
    public ResponseEntity<ApiResponse<MealPlanResponse>> get(@PathVariable UUID planId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(mealPlanService.getFullPlan(planId, coachId)));
    }

    @PutMapping("/api/v1/meal-plans/{planId}")
    @Operation(summary = "Update meal plan metadata")
    public ResponseEntity<ApiResponse<MealPlanSummaryResponse>> update(
            @PathVariable UUID planId,
            @Valid @RequestBody UpdateMealPlanRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Meal plan updated", mealPlanService.updatePlan(planId, coachId, request)));
    }

    @PatchMapping("/api/v1/meal-plans/{planId}/status")
    @Operation(summary = "Update meal plan status (DRAFT → ACTIVE → COMPLETED → ARCHIVED)")
    public ResponseEntity<ApiResponse<MealPlanSummaryResponse>> updateStatus(
            @PathVariable UUID planId,
            @RequestParam MealPlan.Status status) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Status updated", mealPlanService.updateStatus(planId, coachId, status)));
    }

    @DeleteMapping("/api/v1/meal-plans/{planId}")
    @Operation(summary = "Delete meal plan (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID planId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        mealPlanService.deletePlan(planId, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Meal plan deleted", null));
    }

    // ── Days ──────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/meal-plans/{planId}/days")
    @Operation(summary = "Add a day (1–7) to a meal plan")
    public ResponseEntity<ApiResponse<MealPlanDayResponse>> addDay(
            @PathVariable UUID planId,
            @RequestParam int dayNumber) {
        UUID coachId = securityUtils.getCurrentCoachId();
        MealPlanDayResponse day = mealPlanService.addDay(planId, coachId, dayNumber);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Day added", day));
    }

    @DeleteMapping("/api/v1/meal-plans/{planId}/days/{dayId}")
    @Operation(summary = "Remove a day and all its meals from the plan")
    public ResponseEntity<ApiResponse<Void>> removeDay(
            @PathVariable UUID planId,
            @PathVariable UUID dayId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        mealPlanService.removeDay(planId, coachId, dayId);
        return ResponseEntity.ok(ApiResponse.ok("Day removed", null));
    }

    // ── Meals ─────────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/meal-plans/{planId}/days/{dayId}/meals")
    @Operation(summary = "Add a meal to a day")
    public ResponseEntity<ApiResponse<MealResponse>> addMeal(
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @Valid @RequestBody AddMealRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        MealResponse meal = mealPlanService.addMeal(planId, coachId, dayId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Meal added", meal));
    }

    @PutMapping("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}")
    @Operation(summary = "Update a meal")
    public ResponseEntity<ApiResponse<MealResponse>> updateMeal(
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @PathVariable UUID mealId,
            @Valid @RequestBody UpdateMealRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Meal updated",
                mealPlanService.updateMeal(planId, coachId, dayId, mealId, request)));
    }

    @DeleteMapping("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}")
    @Operation(summary = "Remove a meal and its food items")
    public ResponseEntity<ApiResponse<Void>> removeMeal(
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @PathVariable UUID mealId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        mealPlanService.removeMeal(planId, coachId, dayId, mealId);
        return ResponseEntity.ok(ApiResponse.ok("Meal removed", null));
    }

    // ── Meal Items ────────────────────────────────────────────────────────────

    @PostMapping("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}/items")
    @Operation(summary = "Add a food item to a meal (auto-calculates nutrition)")
    public ResponseEntity<ApiResponse<MealItemResponse>> addItem(
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @PathVariable UUID mealId,
            @Valid @RequestBody AddMealItemRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        MealItemResponse item = mealPlanService.addItem(planId, coachId, dayId, mealId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Item added", item));
    }

    @DeleteMapping("/api/v1/meal-plans/{planId}/days/{dayId}/meals/{mealId}/items/{itemId}")
    @Operation(summary = "Remove a food item from a meal (auto-recalculates day totals)")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @PathVariable UUID mealId,
            @PathVariable UUID itemId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        mealPlanService.removeItem(planId, coachId, dayId, mealId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed", null));
    }
}
