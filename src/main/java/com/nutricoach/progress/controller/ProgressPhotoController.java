package com.nutricoach.progress.controller;

import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.response.ApiResponse;
import com.nutricoach.common.security.SecurityUtils;
import com.nutricoach.progress.dto.InitiatePhotoUploadRequest;
import com.nutricoach.progress.dto.PhotoResponse;
import com.nutricoach.progress.dto.PhotoUploadResponse;
import com.nutricoach.progress.entity.ProgressPhoto;
import com.nutricoach.progress.repository.ProgressLogRepository;
import com.nutricoach.progress.repository.ProgressPhotoRepository;
import com.nutricoach.progress.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clients/{clientId}/progress/{logId}/photos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COACH')")
@Tag(name = "Progress Photos", description = "S3-backed progress photo upload and management")
@SecurityRequirement(name = "bearerAuth")
public class ProgressPhotoController {

    private final S3Service s3Service;
    private final ProgressPhotoRepository progressPhotoRepository;
    private final ProgressLogRepository progressLogRepository;
    private final SecurityUtils securityUtils;

    @PostMapping
    @Transactional
    @Operation(summary = "Initiate photo upload", description = "Saves a photo record and returns a pre-signed S3 URL for direct upload")
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> initiateUpload(
            @PathVariable UUID clientId,
            @PathVariable UUID logId,
            @Valid @RequestBody InitiatePhotoUploadRequest request) {

        UUID coachId = securityUtils.getCurrentCoachId();
        requireLogOwned(logId, coachId);

        ProgressPhoto.PhotoType photoType = parsePhotoType(request.photoType());

        S3Service.PresignedUploadResult upload = s3Service.presignUpload(coachId, logId, photoType, request.contentType());

        ProgressPhoto photo = ProgressPhoto.builder()
                .coachId(coachId)
                .progressLogId(logId)
                .s3Key(upload.s3Key())
                .photoType(photoType)
                .build();

        ProgressPhoto saved = progressPhotoRepository.save(photo);

        PhotoUploadResponse response = new PhotoUploadResponse(
                saved.getId(),
                upload.uploadUrl(),
                saved.getS3Key(),
                saved.getPhotoType().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Photo record created — upload to the provided URL", response));
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "List photos for a progress log", description = "Returns all photos with pre-signed download URLs (valid 60 min)")
    public ResponseEntity<ApiResponse<List<PhotoResponse>>> listPhotos(
            @PathVariable UUID clientId,
            @PathVariable UUID logId) {

        UUID coachId = securityUtils.getCurrentCoachId();
        requireLogOwned(logId, coachId);

        List<PhotoResponse> photos = progressPhotoRepository
                .findByCoachIdAndProgressLogIdOrderByCreatedAtAsc(coachId, logId)
                .stream()
                .map(photo -> new PhotoResponse(
                        photo.getId(),
                        photo.getPhotoType().name(),
                        s3Service.presignDownload(photo.getS3Key()),
                        photo.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(photos));
    }

    @DeleteMapping("/{photoId}")
    @Transactional
    @Operation(summary = "Delete a progress photo", description = "Deletes the photo from S3 and removes the DB record")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable UUID clientId,
            @PathVariable UUID logId,
            @PathVariable UUID photoId) {

        UUID coachId = securityUtils.getCurrentCoachId();

        ProgressPhoto photo = progressPhotoRepository.findByIdAndCoachId(photoId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Photo not found"));

        s3Service.deleteObject(photo.getS3Key());
        progressPhotoRepository.deleteByIdAndCoachId(photoId, coachId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void requireLogOwned(UUID logId, UUID coachId) {
        progressLogRepository.findByIdAndCoachId(logId, coachId)
                .orElseThrow(() -> NutriCoachException.notFound("Progress log not found"));
    }

    private ProgressPhoto.PhotoType parsePhotoType(String value) {
        try {
            return ProgressPhoto.PhotoType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw NutriCoachException.badRequest("Invalid photoType — must be FRONT, SIDE, or BACK");
        }
    }
}
