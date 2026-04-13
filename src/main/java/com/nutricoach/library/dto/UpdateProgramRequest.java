package com.nutricoach.library.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateProgramRequest(
        @Size(max = 150) String name,
        String description,
        @Min(1) @Max(365) Integer durationDays) {}
