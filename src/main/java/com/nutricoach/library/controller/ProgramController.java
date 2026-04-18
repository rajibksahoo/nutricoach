package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.*;
import com.nutricoach.library.service.ProgramService;
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
@Tag(name = "Library — Programs", description = "Coach-owned multi-day fitness programs")
@RequestMapping("/api/v1/library/programs")
public class ProgramController {

    private final ProgramService programService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create a program")
    public ResponseEntity<ApiResponse<ProgramSummaryResponse>> create(@Valid @RequestBody CreateProgramRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Program created", programService.create(coachId, req)));
    }

    @GetMapping
    @Operation(summary = "List all programs for the current coach")
    public ResponseEntity<ApiResponse<List<ProgramSummaryResponse>>> list() {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(programService.list(coachId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a program with its day-by-day workout schedule")
    public ResponseEntity<ApiResponse<ProgramResponse>> get(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(programService.get(id, coachId)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update program metadata")
    public ResponseEntity<ApiResponse<ProgramSummaryResponse>> update(@PathVariable UUID id,
                                                                      @Valid @RequestBody UpdateProgramRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Program updated", programService.update(id, coachId, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a program (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        programService.delete(id, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Program deleted", null));
    }

    @PutMapping("/{id}/days/{dayNumber}")
    @Operation(summary = "Assign a workout to a day (or set notes); upserts the day")
    public ResponseEntity<ApiResponse<ProgramResponse>> setDay(@PathVariable UUID id,
                                                               @PathVariable int dayNumber,
                                                               @Valid @RequestBody SetProgramDayRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Day updated", programService.setDay(id, coachId, dayNumber, req)));
    }

    @DeleteMapping("/{id}/days/{dayNumber}")
    @Operation(summary = "Clear a day (remove workout assignment and notes)")
    public ResponseEntity<ApiResponse<ProgramResponse>> clearDay(@PathVariable UUID id,
                                                                 @PathVariable int dayNumber) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Day cleared", programService.clearDay(id, coachId, dayNumber)));
    }
}
