package com.nutricoach.plans.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.plans.dto.FoodItemResponse;
import com.nutricoach.plans.entity.FoodItem;
import com.nutricoach.plans.service.FoodItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/food-items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Food Items", description = "Indian food database (IFCT) — search and lookup")
@SecurityRequirement(name = "bearerAuth")
public class FoodItemController {

    private final FoodItemService foodItemService;

    @GetMapping
    @Operation(
        summary = "Search food items",
        description = "Search by name/Hindi name. Filter by cuisine type and/or category. Returns all items when no params are provided."
    )
    public ResponseEntity<ApiResponse<List<FoodItemResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) FoodItem.CuisineType cuisine,
            @RequestParam(required = false) FoodItem.Category category) {
        return ResponseEntity.ok(ApiResponse.ok(foodItemService.search(q, cuisine, category)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get food item by ID")
    public ResponseEntity<ApiResponse<FoodItemResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(foodItemService.findById(id)));
    }
}
