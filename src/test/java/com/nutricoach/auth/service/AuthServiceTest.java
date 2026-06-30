package com.nutricoach.auth.service;

import com.nutricoach.auth.entity.OtpRequest;
import com.nutricoach.auth.repository.OtpRequestRepository;
import com.nutricoach.coach.repository.CoachRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OtpRequestRepository otpRequestRepository;
    @Mock
    private CoachRepository coachRepository;
    @Mock
    private Msg91Service msg91Service;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        java.lang.reflect.Field devModeField = AuthService.class.getDeclaredField("devMode");
        devModeField.setAccessible(true);
        devModeField.set(authService, false);
    }

    @Test
    void sendOtp_CooldownActive_ThrowsException() {
        OtpRequest lastRequest = new OtpRequest();
        lastRequest.setCreatedAt(Instant.now().minusSeconds(30));
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.of(lastRequest));
        
        assertThrows(NutriCoachException.class, () -> authService.sendOtp("1234567890"));
    }

    @Test
    void sendOtp_Success() {
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-otp");
        
        authService.sendOtp("1234567890");
        
        verify(otpRequestRepository).deleteUnverifiedByPhone("1234567890");
        verify(otpRequestRepository).save(any(OtpRequest.class));
        verify(msg91Service).sendOtp(eq("1234567890"), anyString());
    }

    @Test
    void verifyOtp_NoOtpFound_ThrowsException() {
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.empty());
        assertThrows(NutriCoachException.class, () -> authService.verifyOtp("1234567890", "123456", "Test"));
    }

    @Test
    void verifyOtp_AlreadyVerified_ThrowsException() {
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setVerified(true);
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.of(otpRequest));
        assertThrows(NutriCoachException.class, () -> authService.verifyOtp("1234567890", "123456", "Test"));
    }

    @Test
    void verifyOtp_Expired_ThrowsException() {
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setVerified(false);
        otpRequest.setExpiresAt(Instant.now().minusSeconds(10));
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.of(otpRequest));
        assertThrows(NutriCoachException.class, () -> authService.verifyOtp("1234567890", "123456", "Test"));
    }

    @Test
    void verifyOtp_MaxAttempts_ThrowsException() {
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setVerified(false);
        otpRequest.setExpiresAt(Instant.now().plusSeconds(600));
        otpRequest.setAttempts(3);
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.of(otpRequest));
        assertThrows(NutriCoachException.class, () -> authService.verifyOtp("1234567890", "123456", "Test"));
    }

    @Test
    void verifyOtp_IncorrectOtp_IncrementsAttempts() {
        OtpRequest otpRequest = new OtpRequest();
        otpRequest.setVerified(false);
        otpRequest.setExpiresAt(Instant.now().plusSeconds(600));
        otpRequest.setAttempts(0);
        otpRequest.setOtpHash("hash");
        
        when(otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc("1234567890")).thenReturn(Optional.of(otpRequest));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(false);
        
        NutriCoachException ex = assertThrows(NutriCoachException.class, () -> authService.verifyOtp("1234567890", "123456", "Test"));
        assertTrue(ex.getMessage().contains("Incorrect OTP"));
        assertEquals(1, otpRequest.getAttempts());
        verify(otpRequestRepository).save(otpRequest);
    }

    @Test
    void demoLogin_ProdMode_ThrowsException() {
        assertThrows(NutriCoachException.class, () -> authService.demoLogin("1234567890", "Test"));
    }
}
