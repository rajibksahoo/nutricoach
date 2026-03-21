package com.nutricoach.coach.controller;

import com.nutricoach.coach.dto.CoachResponse;
import com.nutricoach.coach.dto.DashboardResponse;
import com.nutricoach.coach.dto.UpdateCoachRequest;
import com.nutricoach.coach.service.CoachService;
import com.nutricoach.coach.service.DashboardService;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coach")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Coach", description = "Coach profile management")
@SecurityRequirement(name = "bearerAuth")
public class CoachController {

    private final CoachService coachService;
    private final DashboardService dashboardService;
    private final SecurityUtils securityUtils;

    @GetMapping("/dashboard")
    @Operation(summary = "Get coach dashboard",
               description = "Returns client counts by status, meal plan totals, clients needing a plan, and 5 most recent clients")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(
                dashboardService.getDashboard(securityUtils.getCurrentCoachId())
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Get coach profile")
    public ResponseEntity<ApiResponse<CoachResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(
                coachService.getProfile(securityUtils.getCurrentCoachId())
        ));
    }

    @PutMapping("/me")
    @Operation(summary = "Update coach profile", description = "Only provided fields are updated (partial update)")
    public ResponseEntity<ApiResponse<CoachResponse>> updateProfile(
            @Valid @RequestBody UpdateCoachRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated",
                coachService.updateProfile(securityUtils.getCurrentCoachId(), request)
        ));
    }
}
