package com.nutricoach.auth.controller;

import com.nutricoach.auth.dto.AuthResponse;
import com.nutricoach.auth.dto.SendOtpRequest;
import com.nutricoach.auth.dto.VerifyOtpRequest;
import com.nutricoach.auth.service.AuthService;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "OTP-based phone authentication for coaches")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/send")
    @Operation(summary = "Send OTP", description = "Sends a 6-digit OTP to the given Indian mobile number via SMS")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.phone());
        return ResponseEntity.ok(ApiResponse.ok("OTP sent successfully", null));
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Verifies OTP and returns a JWT. Creates coach account on first login.")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyOtp(request.phone(), request.otp(), request.name());
        String message = response.isNewCoach() ? "Welcome to NutriCoach!" : "Login successful";
        return ResponseEntity.ok(ApiResponse.ok(message, response));
    }
}
