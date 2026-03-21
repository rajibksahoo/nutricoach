package com.nutricoach.client.dto;

import com.nutricoach.client.entity.Client;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateClientRequest(

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @Email(message = "Enter a valid email address")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit WhatsApp number")
        String whatsappNumber,

        LocalDate dateOfBirth,

        Client.Gender gender,

        @Min(value = 50, message = "Height must be at least 50 cm")
        @Max(value = 300, message = "Height must be 300 cm or less")
        Integer heightCm,

        @DecimalMin(value = "10.0", message = "Weight must be at least 10 kg")
        @DecimalMax(value = "500.0", message = "Weight must be 500 kg or less")
        BigDecimal weightKg,

        Client.Goal goal,

        Client.DietaryPref dietaryPref,

        Client.ActivityLevel activityLevel,

        List<String> healthConditions,

        List<String> allergies
) {}
