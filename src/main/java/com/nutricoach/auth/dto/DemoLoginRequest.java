package com.nutricoach.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DemoLoginRequest(

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        // Optional — used as display name for new accounts created via demo login
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name
) {}
