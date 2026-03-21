package com.nutricoach.client.dto;

import com.nutricoach.client.entity.Client;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String phone,
        String name,
        String email,
        String whatsappNumber,
        LocalDate dateOfBirth,
        Client.Gender gender,
        Integer heightCm,
        BigDecimal weightKg,
        Client.Goal goal,
        Client.DietaryPref dietaryPref,
        Client.ActivityLevel activityLevel,
        List<String> healthConditions,
        List<String> allergies,
        Client.Status status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(
                c.getId(),
                c.getPhone(),
                c.getName(),
                c.getEmail(),
                c.getWhatsappNumber(),
                c.getDateOfBirth(),
                c.getGender(),
                c.getHeightCm(),
                c.getWeightKg(),
                c.getGoal(),
                c.getDietaryPref(),
                c.getActivityLevel(),
                c.getHealthConditions(),
                c.getAllergies(),
                c.getStatus(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
