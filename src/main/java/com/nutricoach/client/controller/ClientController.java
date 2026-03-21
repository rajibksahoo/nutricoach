package com.nutricoach.client.controller;

import com.nutricoach.client.dto.ClientResponse;
import com.nutricoach.client.dto.CreateClientRequest;
import com.nutricoach.client.dto.UpdateClientRequest;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.service.ClientService;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
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
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Clients", description = "Manage clients (multi-tenant — coach-scoped)")
@SecurityRequirement(name = "bearerAuth")
public class ClientController {

    private final ClientService clientService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "List clients", description = "Returns all active clients for the authenticated coach. Filter by status using ?status=ACTIVE|ONBOARDING|INACTIVE")
    public ResponseEntity<ApiResponse<List<ClientResponse>>> list(
            @RequestParam(required = false) Client.Status status) {
        UUID coachId = securityUtils.getCurrentCoachId();
        List<ClientResponse> clients = clientService.findAll(coachId, status);
        return ResponseEntity.ok(ApiResponse.ok(clients));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get client by ID")
    public ResponseEntity<ApiResponse<ClientResponse>> get(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        ClientResponse client = clientService.findById(id, coachId);
        return ResponseEntity.ok(ApiResponse.ok(client));
    }

    @PostMapping
    @Operation(summary = "Create client")
    public ResponseEntity<ApiResponse<ClientResponse>> create(@Valid @RequestBody CreateClientRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        ClientResponse client = clientService.create(request, coachId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Client created", client));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update client")
    public ResponseEntity<ApiResponse<ClientResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClientRequest request) {
        UUID coachId = securityUtils.getCurrentCoachId();
        ClientResponse client = clientService.update(id, request, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Client updated", client));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete client (soft delete)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID coachId = securityUtils.getCurrentCoachId();
        clientService.delete(id, coachId);
        return ResponseEntity.ok(ApiResponse.ok("Client deleted", null));
    }
}
