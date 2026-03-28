package com.nutricoach.auth.service;

import com.nutricoach.common.exception.NutriCoachException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Enforces a limit of 5 OTP send requests per phone number per hour.
     * Throws a 429 NutriCoachException if the limit is exceeded.
     *
     * @param phone the phone number to rate-limit
     */
    public void checkOtpSendLimit(String phone) {
        Bucket bucket = buckets.computeIfAbsent("send:" + phone, k -> buildBucket(5));
        if (!bucket.tryConsume(1)) {
            throw NutriCoachException.tooManyRequests("Too many requests. Please try again later.");
        }
    }

    /**
     * Enforces a limit of 10 OTP verify requests per phone number per hour.
     * Throws a 429 NutriCoachException if the limit is exceeded.
     *
     * @param phone the phone number to rate-limit
     */
    public void checkOtpVerifyLimit(String phone) {
        Bucket bucket = buckets.computeIfAbsent("verify:" + phone, k -> buildBucket(10));
        if (!bucket.tryConsume(1)) {
            throw NutriCoachException.tooManyRequests("Too many requests. Please try again later.");
        }
    }

    private Bucket buildBucket(int capacity) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofHours(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
