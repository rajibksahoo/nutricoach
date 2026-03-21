package com.nutricoach.progress.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.progress.dto.LogProgressRequest;
import com.nutricoach.progress.dto.ProgressLogResponse;
import com.nutricoach.progress.service.ProgressService;
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
@RequestMapping("/api/v1/clients/{clientId}/progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Progress", description = "Client progress logs and measurements")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progressService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Log progress", description = "Creates or updates the progress entry for the given date (one per client per day)")
    public ResponseEntity<ApiResponse<ProgressLogResponse>> log(
            @PathVariable UUID clientId,
            @Valid @RequestBody LogProgressRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Progress logged", progressService.log(clientId, coachId, request)));
    }

    @GetMapping
    @Operation(summary = "Get full progress history", description = "Returns all progress logs sorted by date descending")
    public ResponseEntity<ApiResponse<List<ProgressLogResponse>>> getHistory(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(progressService.getHistory(clientId, coachId)));
    }

    @GetMapping("/chart")
    @Operation(summary = "Get progress chart data", description = "Returns progress logs for the last N days (default 30, max 90), sorted ascending for charting")
    public ResponseEntity<ApiResponse<List<ProgressLogResponse>>> getChart(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 90) days = 30;
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(progressService.getChart(clientId, coachId, days)));
    }
}
