package com.nutricoach.auth.controller;

import com.nutricoach.auth.dto.ClientAuthResponse;
import com.nutricoach.auth.dto.ClientVerifyOtpRequest;
import com.nutricoach.auth.dto.SendOtpRequest;
import com.nutricoach.auth.service.AuthService;
import com.nutricoach.auth.service.ClientAuthService;
import com.nutricoach.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client-auth")
@RequiredArgsConstructor
@Tag(name = "Client Authentication", description = "OTP-based phone authentication for clients (coach's end-customers)")
public class ClientAuthController {

    private final AuthService authService;
    private final ClientAuthService clientAuthService;

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP to client", description = "Sends a 6-digit OTP to the client's registered mobile number")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.phone());
        return ResponseEntity.ok(ApiResponse.ok("OTP sent successfully", null));
    }

    @PostMapping("/otp/verify")
    @Operation(
        summary = "Verify client OTP",
        description = "Verifies OTP for a client registered under the specified coach. Returns a CLIENT-role JWT."
    )
    public ResponseEntity<ApiResponse<ClientAuthResponse>> verifyOtp(
            @Valid @RequestBody ClientVerifyOtpRequest request) {
        ClientAuthResponse response = clientAuthService.verifyOtp(
                request.phone(), request.otp(), request.coachId());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }
}
