package com.nutricoach.auth.service;

import com.nutricoach.auth.dto.ClientAuthResponse;
import com.nutricoach.auth.entity.OtpRequest;
import com.nutricoach.auth.repository.OtpRequestRepository;
import com.nutricoach.client.entity.Client;
import com.nutricoach.client.repository.ClientRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAuthService {

    private static final int MAX_VERIFY_ATTEMPTS = 3;
    private static final String DEV_OTP = "111111";

    private final OtpRequestRepository otpRequestRepository;
    private final ClientRepository clientRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;

    @Value("${app.msg91.dev-mode:false}")
    private boolean devMode;

    @Transactional(noRollbackFor = NutriCoachException.class)
    public ClientAuthResponse verifyOtp(String phone, String otp, UUID coachId) {
        rateLimiterService.checkOtpVerifyLimit(phone);

        if (devMode && DEV_OTP.equals(otp)) {
            log.info("[DEV] Universal OTP bypass for client: {}", phone);
            return issueTokenForClient(phone, coachId);
        }

        OtpRequest otpRequest = otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> NutriCoachException.badRequest(
                        "No OTP found for this number. Please request a new OTP."));

        if (otpRequest.isVerified()) {
            throw NutriCoachException.badRequest("OTP already used. Please request a new OTP.");
        }

        if (Instant.now().isAfter(otpRequest.getExpiresAt())) {
            throw NutriCoachException.badRequest("OTP has expired. Please request a new OTP.");
        }

        if (otpRequest.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
            throw NutriCoachException.badRequest("Too many incorrect attempts. Please request a new OTP.");
        }

        if (!passwordEncoder.matches(otp, otpRequest.getOtpHash())) {
            otpRequest.setAttempts(otpRequest.getAttempts() + 1);
            otpRequestRepository.save(otpRequest);
            int remaining = MAX_VERIFY_ATTEMPTS - otpRequest.getAttempts();
            throw NutriCoachException.badRequest("Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        otpRequest.setVerified(true);
        otpRequestRepository.save(otpRequest);

        return issueTokenForClient(phone, coachId);
    }

    private ClientAuthResponse issueTokenForClient(String phone, UUID coachId) {
        Client client = clientRepository.findByPhoneAndCoachIdAndDeletedAtIsNull(phone, coachId)
                .orElseThrow(() -> NutriCoachException.notFound(
                        "No client account found for this number with this coach"));

        String token = jwtService.generateClientToken(phone, client.getId(), coachId);
        return new ClientAuthResponse(token, client.getId(), coachId, client.getName(), client.getPhone());
    }
}
