package com.nutricoach.progress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record InitiatePhotoUploadRequest(

        @NotNull(message = "photoType is required")
        String photoType,

        @NotBlank(message = "contentType is required")
        @Pattern(regexp = "image/(jpeg|png|webp)", message = "contentType must be image/jpeg, image/png, or image/webp")
        String contentType
) {}
