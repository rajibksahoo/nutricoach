package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.WorkoutResponse;
import com.nutricoach.library.dto.WorkoutTemplateResponse;
import com.nutricoach.library.service.WorkoutTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Library — Workout Templates", description = "System-seeded workout templates")
public class WorkoutTemplateController {

    private final WorkoutTemplateService templateService;
    private final SecurityUtils securityUtils;

    @GetMapping("/api/v1/library/workout-templates")
    @Operation(summary = "List all workout templates")
    public ResponseEntity<ApiResponse<List<WorkoutTemplateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(templateService.list()));
    }

    @GetMapping("/api/v1/library/workout-templates/{id}")
    @Operation(summary = "Get a single workout template")
    public ResponseEntity<ApiResponse<WorkoutTemplateResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.get(id)));
    }

    @PostMapping("/api/v1/library/workout-templates/{id}/instantiate")
    @Operation(summary = "Create a real workout for the current coach from this template")
    public ResponseEntity<ApiResponse<WorkoutResponse>> instantiate(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Workout created from template", templateService.instantiate(id, coachId)));
    }
}
