package com.nutricoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
        String otp,

        // Optional — required only for new coaches. If blank, phone is used as placeholder name.
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name
) {}
