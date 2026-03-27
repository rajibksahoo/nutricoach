package com.nutricoach.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NutriCoachException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public NutriCoachException(String message, String errorCode, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public static NutriCoachException notFound(String message) {
        return new NutriCoachException(message, "NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public static NutriCoachException badRequest(String message) {
        return new NutriCoachException(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }

    public static NutriCoachException unauthorized(String message) {
        return new NutriCoachException(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    public static NutriCoachException conflict(String message) {
        return new NutriCoachException(message, "CONFLICT", HttpStatus.CONFLICT);
    }

    public static NutriCoachException forbidden(String message) {
        return new NutriCoachException(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    public static NutriCoachException paymentRequired(String message) {
        return new NutriCoachException(message, "PAYMENT_REQUIRED", HttpStatus.PAYMENT_REQUIRED);
    }
}