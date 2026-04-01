package com.nutricoach.client.controller;

import com.nutricoach.client.dto.ClientResponse;
import com.nutricoach.client.service.ClientService;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portal/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Client Portal — Profile", description = "Read-only client profile")
public class ClientProfileController {

    private final ClientService clientService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Get my profile", description = "Returns the authenticated client's own profile")
    public ResponseEntity<ApiResponse<ClientResponse>> getProfile() {
        UUID clientId = securityUtils.getCurrentClientId();
        UUID coachId  = securityUtils.getCurrentCoachIdFromToken();
        return ResponseEntity.ok(ApiResponse.ok(clientService.findById(clientId, coachId)));
    }
}
