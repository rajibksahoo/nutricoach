package com.nutricoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClientVerifyOtpRequest(

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be 6 digits")
        String otp,

        /**
         * Optional. A client's phone identifies exactly one coach (changeset
         * 028), so the portal no longer needs it. Still accepted because the
         * web portal and the Android app both send it today.
         */
        UUID coachId
) {}
