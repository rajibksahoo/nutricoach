package com.nutricoach.auth.service;

import com.nutricoach.auth.entity.OtpRequest;
import com.nutricoach.auth.repository.OtpRequestRepository;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientAuthServiceTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private ClientAuthService clientAuthService;

    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field devModeField = ClientAuthService.class.getDeclaredField("devMode");
        devModeField.setAccessible(true);
        devModeField.set(clientAuthService, false);
    }

    @Test
    void verifyOtp_NoOtpFound_ThrowsException() {
        UUID coachId = UUID.randomUUID();
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.empty());
        assertThrows(NutriCoachException.class, () -> clientAuthService.verifyOtp("1234567890", "123456", coachId));
    }

    @Test
    void verifyOtp_IncorrectOtp_IncrementsAttempts() {
        UUID coachId = UUID.randomUUID();
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setVerified(false);
        otpRequest.setExpiresAt(Instant.now().plusSeconds(600));
        otpRequest.setAttempts(1);
        otpRequest.setOtpHash("hash");
        
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.of(otpRequest));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(false);
        
        NutriCoachException ex = assertThrows(NutriCoachException.class, () -> clientAuthService.verifyOtp("1234567890", "123456", coachId));
        assertTrue(ex.getMessage().contains("Incorrect OTP"));
        assertEquals(2, otpRequest.getAttempts());
        verify(otpRequestRepository).save(otpRequest);
    }

    @Test
    void verifyOtp_ClientNotFound_ThrowsException() {
        UUID coachId = UUID.randomUUID();
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setVerified(false);
        otpRequest.setExpiresAt(Instant.now().plusSeconds(600));
        otpRequest.setAttempts(0);
        otpRequest.setOtpHash("hash");
        
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.of(otpRequest));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(clientRepository.findByPhoneAndCoachIdAndDeletedAtIsNull("1234567890", coachId)).thenReturn(Optional.empty());
        
        NutriCoachException ex = assertThrows(NutriCoachException.class, () -> clientAuthService.verifyOtp("1234567890", "123456", coachId));
        assertTrue(ex.getMessage().contains("No client account found"));
    }
}
