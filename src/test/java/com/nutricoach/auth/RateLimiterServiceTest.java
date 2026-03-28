package com.nutricoach.auth;

import com.nutricoach.auth.service.RateLimiterService;
import com.nutricoach.common.exception.NutriCoachException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for RateLimiterService. Uses a fresh instance per test to avoid
 * shared bucket state between test cases.
 */
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setup() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    void checkOtpSendLimit_firstFiveRequests_passes() {
        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> rateLimiterService.checkOtpSendLimit("9876543210"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void checkOtpSendLimit_sixthRequest_throws429() {
        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkOtpSendLimit("9876543211");
        }
        assertThatThrownBy(() -> rateLimiterService.checkOtpSendLimit("9876543211"))
                .isInstanceOf(NutriCoachException.class)
                .hasMessageContaining("Too many requests");
    }

    @Test
    void checkOtpVerifyLimit_firstTenRequests_passes() {
        for (int i = 0; i < 10; i++) {
            assertThatCode(() -> rateLimiterService.checkOtpVerifyLimit("9876543220"))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void checkOtpVerifyLimit_eleventhRequest_throws429() {
        for (int i = 0; i < 10; i++) {
            rateLimiterService.checkOtpVerifyLimit("9876543221");
        }
        assertThatThrownBy(() -> rateLimiterService.checkOtpVerifyLimit("9876543221"))
                .isInstanceOf(NutriCoachException.class)
                .hasMessageContaining("Too many requests");
    }

    @Test
    void differentPhones_haveSeparateBuckets() {
        // Exhaust bucket for phone A
        for (int i = 0; i < 5; i++) {
            rateLimiterService.checkOtpSendLimit("1111111111");
        }
        // Phone B should still pass
        assertThatCode(() -> rateLimiterService.checkOtpSendLimit("2222222222"))
                .doesNotThrowAnyException();
    }
}
