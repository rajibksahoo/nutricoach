package com.nutricoach.progress.controller;

import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.progress.dto.CheckInResponse;
import com.nutricoach.progress.dto.CreateCheckInRequest;
import com.nutricoach.progress.dto.UpdateCheckInRequest;
import com.nutricoach.progress.service.CheckInService;
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
@RequestMapping("/api/v1/clients/{clientId}/check-ins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Check-ins", description = "Weekly meal plan adherence check-ins")
@SecurityRequirement(name = "bearerAuth")
public class CheckInController {

    private final CheckInService checkInService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Operation(summary = "Create check-in", description = "Records a weekly check-in with adherence % against an active meal plan. One check-in per date.")
    public ResponseEntity<ApiResponse<CheckInResponse>> create(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateCheckInRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Check-in recorded", checkInService.create(clientId, coachId, request)));
    }

    @GetMapping
    @Operation(summary = "Get check-in history", description = "Returns all check-ins sorted by date descending")
    public ResponseEntity<ApiResponse<List<CheckInResponse>>> getHistory(@PathVariable UUID clientId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok(checkInService.getHistory(clientId, coachId)));
    }

    @PutMapping("/{checkInId}")
    @Operation(summary = "Edit a check-in",
            description = "Partial update. Use coachNotes to reply to a check-in the client submitted — it could previously only be set at creation.")
    public ResponseEntity<ApiResponse<CheckInResponse>> update(
            @PathVariable UUID clientId,
            @PathVariable UUID checkInId,
            @Valid @RequestBody UpdateCheckInRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        return ResponseEntity.ok(ApiResponse.ok("Check-in updated",
                checkInService.update(clientId, checkInId, coachId, request)));
    }

    @DeleteMapping("/{checkInId}")
    @Operation(summary = "Remove a check-in", description = "Soft delete; it disappears from history, the activity feed and the overdue calculation")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID clientId,
            @PathVariable UUID checkInId) {
        UUID coachId = securityUtils.getCurrentCoachId();
        checkInService.delete(clientId, checkInId, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Check-in removed", null));
    }
}
