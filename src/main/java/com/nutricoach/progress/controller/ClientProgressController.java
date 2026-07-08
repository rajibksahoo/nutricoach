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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portal/progress")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Client Portal — Progress", description = "Read-only progress history for clients")
public class ClientProgressController {

    private final ProgressService progressService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get my progress history", description = "Returns all progress logs sorted by date descending")
    public ResponseEntity<ApiResponse<List<ProgressLogResponse>>> getHistory() {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(progressService.getHistory(clientId, coachId)));
    }

    @GetMapping("/chart")
    @Operation(summary = "Get my progress chart data", description = "Returns progress logs for the last N days (default 30, max 90), sorted ascending for charting")
    public ResponseEntity<ApiResponse<List<ProgressLogResponse>>> getChart(
            @RequestParam(defaultValue = "30") int days) {
        if (days < 1 || days > 90) days = 30;
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(progressService.getChart(clientId, coachId, days)));
    }

    @PostMapping
    @Operation(summary = "Log a progress entry", description = "Logs (or upserts for the day) a progress entry for the authenticated client")
    public ResponseEntity<ApiResponse<ProgressLogResponse>> log(
            @Valid @RequestBody LogProgressRequest request) {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        ProgressLogResponse entry = progressService.log(clientId, coachId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Progress logged", entry));
    }
}
