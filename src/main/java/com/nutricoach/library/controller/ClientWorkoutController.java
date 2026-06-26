package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.ClientScheduledWorkoutResponse;
import com.nutricoach.library.service.PortalWorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portal/workouts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Client Portal — Workouts", description = "Upcoming workouts from the client's assigned programs")
public class ClientWorkoutController {

    private final PortalWorkoutService portalWorkoutService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get my upcoming workouts",
            description = "Derived live from assigned programs: each program day's workout dated from the assignment start date, today onward")
    public ResponseEntity<ApiResponse<List<ClientScheduledWorkoutResponse>>> getUpcoming() {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(portalWorkoutService.listUpcoming(clientId, coachId)));
    }
}
