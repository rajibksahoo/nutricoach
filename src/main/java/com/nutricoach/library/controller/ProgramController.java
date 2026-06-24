package com.nutricoach.library.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.library.dto.*;
import com.nutricoach.library.service.ProgramAssignmentService;
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
    private final ProgramAssignmentService programAssignmentService;
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

    @PostMapping("/{id}/cover")
    @Operation(summary = "Initiate a cover-image upload", description = "Stores the S3 key and returns a pre-signed PUT URL for direct upload")
    public ResponseEntity<ApiResponse<ProgramCoverUploadResponse>> initiateCoverUpload(
            @PathVariable UUID id, @Valid @RequestBody ProgramCoverUploadRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Upload to the provided URL", programService.initiateCoverUpload(id, coachId, req)));
    }

    @DeleteMapping("/{id}/cover")
    @Operation(summary = "Remove a program's cover image")
    public ResponseEntity<ApiResponse<Void>> deleteCover(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        programService.deleteCover(id, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Cover removed", null));
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Assign a program to one or more clients")
    public ResponseEntity<ApiResponse<List<ProgramAssignmentResponse>>> assign(
            @PathVariable UUID id, @Valid @RequestBody AssignProgramRequest req) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Program assigned", programAssignmentService.assign(id, coachId, req)));
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "List active assignments for a program")
    public ResponseEntity<ApiResponse<List<ProgramAssignmentResponse>>> listAssignments(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(programAssignmentService.listByProgram(id, coachId)));
    }

    @DeleteMapping("/{id}/assignments/{assignmentId}")
    @Operation(summary = "Soft-delete a program assignment")
    public ResponseEntity<ApiResponse<Void>> unassign(@PathVariable UUID id, @PathVariable UUID assignmentId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        programAssignmentService.unassign(id, assignmentId, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Assignment removed", null));
    }
}
