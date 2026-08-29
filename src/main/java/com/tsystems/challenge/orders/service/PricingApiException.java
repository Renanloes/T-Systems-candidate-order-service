package com.tsystems.challenge.orders.service;

public class PricingApiException extends RuntimeException {

    private final Integer statusCode;

    public PricingApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public PricingApiException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public PricingApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public Integer statusCode() {
        return statusCode;
    }

    public boolean requiresAttention() {
        return statusCode != null && statusCode >= 400 && statusCode < 500;
    }
}