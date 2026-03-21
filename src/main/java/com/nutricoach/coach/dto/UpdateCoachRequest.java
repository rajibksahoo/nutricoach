package com.nutricoach.coach.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCoachRequest(

        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Email(message = "Invalid email address")
        String email,

        @Size(max = 150, message = "Business name must be under 150 characters")
        String businessName,

        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                 message = "Invalid GSTIN format")
        String gstin
) {}
