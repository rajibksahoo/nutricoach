package com.nutricoach.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProgramRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Min(1) @Max(52) Integer weeks,
        @Min(1) @Max(365) Integer durationDays,
        @Size(max = 60) String modality,
        @Size(max = 40) String experienceLevel,
        List<String> tags) {}
