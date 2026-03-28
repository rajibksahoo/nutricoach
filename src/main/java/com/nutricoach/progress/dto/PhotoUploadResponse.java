package com.nutricoach.progress.dto;

import java.util.UUID;

public record PhotoUploadResponse(UUID photoId, String uploadUrl, String s3Key, String photoType) {
}
