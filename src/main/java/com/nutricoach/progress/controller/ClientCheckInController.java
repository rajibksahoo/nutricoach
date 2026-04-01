package com.nutricoach.progress.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.progress.dto.CheckInResponse;
import com.nutricoach.progress.service.CheckInService;
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
@RequestMapping("/api/v1/portal/check-ins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Client Portal — Check-ins", description = "Read-only check-in history for clients")
public class ClientCheckInController {

    private final CheckInService checkInService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get my check-in history", description = "Returns all check-ins sorted by date descending")
    public ResponseEntity<ApiResponse<List<CheckInResponse>>> getHistory() {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(checkInService.getHistory(clientId, coachId)));
    }
}
