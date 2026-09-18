package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.ClientAssignmentsResponse;
import com.nutricoach.library.service.ClientTrainingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Library — Assignments & Schedules", description = "Assign workouts to clients and schedule them for specific dates")
public class ClientTrainingController {

    private final ClientTrainingService clientTrainingService;
    private final SecurityUtils securityUtils;

    @GetMapping("/api/v1/library/clients/{clientId}/assignments")
    @Operation(summary = "List the programs and workouts assigned to a client (coach-side Training tab)")
    public ResponseEntity<ApiResponse<ClientAssignmentsResponse>> listAssignments(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(clientTrainingService.listForClient(clientId, coachId)));
    }
}
