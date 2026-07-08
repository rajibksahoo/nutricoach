package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.ClientScheduledWorkoutResponse;
import com.nutricoach.library.dto.CompleteWorkoutRequest;
import com.nutricoach.library.service.PortalWorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/complete")
    @Operation(summary = "Mark a scheduled workout as completed",
            description = "Marks one of my derived upcoming/today workouts done. Idempotent — "
                    + "re-completing the same (workout, date) returns success without a duplicate row. "
                    + "Returns the workout with completed=true.")
    public ResponseEntity<ApiResponse<ClientScheduledWorkoutResponse>> complete(
            @Valid @RequestBody CompleteWorkoutRequest request) {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        ClientScheduledWorkoutResponse result =
                portalWorkoutService.complete(clientId, coachId, request.workoutId(), request.date());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }
}
