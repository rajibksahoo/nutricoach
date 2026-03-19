package com.nutricoach.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class Msg91Service {

    private static final String MSG91_OTP_URL = "https://control.msg91.com/api/v5/otp";

    @Value("${app.msg91.auth-key}")
    private String authKey;

    @Value("${app.msg91.template-id}")
    private String templateId;

    @Value("${app.msg91.dev-mode:false}")
    private boolean devMode;

    private final RestClient restClient = RestClient.create();

    /**
     * Sends OTP to the given phone number via MSG91.
     * In dev-mode, just logs the OTP (no real SMS sent).
     *
     * @param phone 10-digit Indian mobile number (without country code)
     * @param otp   6-digit OTP
     */
    public void sendOtp(String phone, String otp) {
        String mobile = "91" + phone; // prepend country code

        if (devMode) {
            log.info("[DEV] OTP for {} → {}", phone, otp);
            return;
        }

        try {
            Map<String, String> body = Map.of(
                    "template_id", templateId,
                    "mobile", mobile,
                    "otp", otp
            );

            restClient.post()
                    .uri(MSG91_OTP_URL)
                    .header("authkey", authKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("OTP sent to {}", phone);
        } catch (Exception e) {
            log.error("Failed to send OTP to {}: {}", phone, e.getMessage());
            throw new RuntimeException("Failed to send OTP. Please try again.");
        }
    }
}
