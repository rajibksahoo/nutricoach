package com.nutricoach.progress.service;

import com.nutricoach.common.config.AwsProperties;
import com.nutricoach.progress.entity.ProgressPhoto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    public record PresignedUploadResult(String uploadUrl, String s3Key) {}

    public PresignedUploadResult presignUpload(UUID coachId, UUID progressLogId,
                                               ProgressPhoto.PhotoType photoType, String contentType) {
        String s3Key = "photos/%s/%s/%s-%s.jpg".formatted(
                coachId, progressLogId, photoType.name().toLowerCase(), UUID.randomUUID());

        if (isLocalDev()) {
            return new PresignedUploadResult("https://local-dummy-upload-url.example.com/" + s3Key, s3Key);
        }

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(put -> put
                        .bucket(awsProperties.getS3().getBucket())
                        .key(s3Key)
                        .contentType(contentType)
                )
        );

        return new PresignedUploadResult(presigned.url().toString(), s3Key);
    }

    public String presignDownload(String s3Key) {
        if (isLocalDev()) {
            return "https://local-dummy-download-url.example.com/" + s3Key;
        }

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(r -> r
                .signatureDuration(Duration.ofMinutes(60))
                .getObjectRequest(get -> get
                        .bucket(awsProperties.getS3().getBucket())
                        .key(s3Key)
                )
        );

        return presigned.url().toString();
    }

    public void deleteObject(String s3Key) {
        if (isLocalDev()) {
            return;
        }

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(awsProperties.getS3().getBucket())
                .key(s3Key)
                .build());
    }

    private boolean isLocalDev() {
        String accessKey = awsProperties.getAccessKey();
        return accessKey != null && accessKey.startsWith("local-");
    }
}
