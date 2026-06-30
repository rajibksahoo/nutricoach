package com.nutricoach.progress.service;

import com.nutricoach.common.config.AwsProperties;
import com.nutricoach.progress.entity.ProgressPhoto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private AwsProperties awsProperties;

    @InjectMocks
    private S3Service s3Service;

    @Test
    void presignUpload_LocalDev_ReturnsDummy() {
        when(awsProperties.getAccessKey()).thenReturn("local-key");
        
        S3Service.PresignedUploadResult result = s3Service.presignUpload("test-key", "image/jpeg");
        
        assertTrue(result.uploadUrl().startsWith("https://local-dummy-upload-url"));
        assertEquals("test-key", result.s3Key());
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void presignUpload_Prod_ReturnsPresignedUrl() throws Exception {
        when(awsProperties.getAccessKey()).thenReturn("prod-key");
        
        AwsProperties.S3 s3Props = new AwsProperties.S3();
        s3Props.setBucket("test-bucket");
        when(awsProperties.getS3()).thenReturn(s3Props);
        
        PresignedPutObjectRequest mockRequest = mock(PresignedPutObjectRequest.class);
        lenient().when(mockRequest.url()).thenReturn(new java.net.URL("https://s3.amazonaws.com/test"));
        lenient().when(s3Presigner.presignPutObject(any(Consumer.class))).thenReturn(mockRequest);
        
        S3Service.PresignedUploadResult result = s3Service.presignUpload("test-key", "image/jpeg");
        
        assertEquals("https://s3.amazonaws.com/test", result.uploadUrl());
        assertEquals("test-key", result.s3Key());
    }

    @Test
    void presignDownload_LocalDev_ReturnsDummy() {
        when(awsProperties.getAccessKey()).thenReturn("local-key");
        
        String url = s3Service.presignDownload("test-key");
        
        assertTrue(url.startsWith("https://local-dummy-download-url"));
        verifyNoInteractions(s3Presigner);
    }
    
    @Test
    void presignDownload_Prod_ReturnsPresignedUrl() throws Exception {
        when(awsProperties.getAccessKey()).thenReturn("prod-key");
        
        AwsProperties.S3 s3Props = new AwsProperties.S3();
        s3Props.setBucket("test-bucket");
        when(awsProperties.getS3()).thenReturn(s3Props);
        
        PresignedGetObjectRequest mockRequest = mock(PresignedGetObjectRequest.class);
        lenient().when(mockRequest.url()).thenReturn(new java.net.URL("https://s3.amazonaws.com/test-get"));
        lenient().when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(mockRequest);
        
        String url = s3Service.presignDownload("test-key");
        
        assertEquals("https://s3.amazonaws.com/test-get", url);
    }

    @Test
    void deleteObject_LocalDev_DoesNothing() {
        when(awsProperties.getAccessKey()).thenReturn("local-key");
        
        assertDoesNotThrow(() -> s3Service.deleteObject("test-key"));
        verifyNoInteractions(s3Client);
    }

    @Test
    void deleteObject_Prod_CallsS3Client() {
        when(awsProperties.getAccessKey()).thenReturn("prod-key");
        
        AwsProperties.S3 s3Props = new AwsProperties.S3();
        s3Props.setBucket("test-bucket");
        when(awsProperties.getS3()).thenReturn(s3Props);
        
        s3Service.deleteObject("test-key");
        
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
    
    @Test
    void presignUpload_WithCoachAndProgressLog_GeneratesKeyAndPresigns() {
        when(awsProperties.getAccessKey()).thenReturn("local-key");
        UUID coachId = UUID.randomUUID();
        UUID progressLogId = UUID.randomUUID();
        
        S3Service.PresignedUploadResult result = s3Service.presignUpload(coachId, progressLogId, ProgressPhoto.PhotoType.FRONT, "image/jpeg");
        
        assertTrue(result.s3Key().contains(coachId.toString()));
        assertTrue(result.s3Key().contains(progressLogId.toString()));
        assertTrue(result.s3Key().contains("front"));
    }
    
    @Test
    void programCoverKey_GeneratesCorrectKey() {
        UUID coachId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        String key = S3Service.programCoverKey(coachId, programId);
        
        assertTrue(key.startsWith("program-covers/" + coachId + "/" + programId));
    }
}
