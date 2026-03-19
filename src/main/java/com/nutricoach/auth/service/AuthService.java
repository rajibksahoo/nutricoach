package com.nutricoach.auth.service;

import com.nutricoach.auth.dto.AuthResponse;
import com.nutricoach.auth.entity.OtpRequest;
import com.nutricoach.auth.repository.OtpRequestRepository;
import com.nutricoach.coach.entity.Coach;
import com.nutricoach.coach.repository.CoachRepository;
import com.nutricoach.common.exception.NutriCoachException;
import com.nutricoach.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_VERIFY_ATTEMPTS = 3;
    private static final int OTP_RESEND_COOLDOWN_SECONDS = 60;

    private final OtpRequestRepository otpRequestRepository;
    private final CoachRepository coachRepository;
    private final Msg91Service msg91Service;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void sendOtp(String phone) {
        // Enforce 60-second cooldown between sends
        otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .ifPresent(last -> {
                    long secondsSinceLast = Instant.now().getEpochSecond() - last.getCreatedAt().getEpochSecond();
                    if (secondsSinceLast < OTP_RESEND_COOLDOWN_SECONDS) {
                        throw NutriCoachException.badRequest(
                                "Please wait " + (OTP_RESEND_COOLDOWN_SECONDS - secondsSinceLast) + " seconds before requesting a new OTP");
                    }
                });

        // Clean up old unverified OTPs for this phone
        otpRequestRepository.deleteUnverifiedByPhone(phone);

        String otp = generateOtp();
        OtpRequest otpRequest = OtpRequest.builder()
                .phone(phone)
                .otpHash(passwordEncoder.encode(otp))
                .expiresAt(Instant.now().plusSeconds(OTP_EXPIRY_MINUTES * 60L))
                .build();

        otpRequestRepository.save(otpRequest);
        msg91Service.sendOtp(phone, otp);
    }

    @Transactional
    public AuthResponse verifyOtp(String phone, String otp, String name) {
        OtpRequest otpRequest = otpRequestRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> NutriCoachException.badRequest("No OTP found for this number. Please request a new OTP."));

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

        boolean isNewCoach = !coachRepository.existsByPhone(phone);
        Coach coach;

        if (isNewCoach) {
            String coachName = StringUtils.hasText(name) ? name.trim() : phone;
            coach = Coach.builder()
                    .phone(phone)
                    .name(coachName)
                    .trialEndsAt(Instant.now().plusSeconds(14 * 24 * 3600L)) // 14-day trial
                    .build();
            coachRepository.save(coach);
            log.info("New coach registered: {}", phone);
        } else {
            coach = coachRepository.findByPhone(phone)
                    .orElseThrow(() -> NutriCoachException.notFound("Coach not found"));
        }

        String token = jwtService.generateToken(coach.getPhone(), coach.getId(), "ROLE_COACH");
        return new AuthResponse(token, coach.getId(), coach.getPhone(), isNewCoach);
    }

    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}
