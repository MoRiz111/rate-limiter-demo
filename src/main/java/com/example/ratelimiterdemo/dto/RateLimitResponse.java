package com.example.ratelimiterdemo.dto;

public class RateLimitResponse {
    private boolean allowed;
    private int remaining;
    private long retryAfter;

    public RateLimitResponse(boolean allowed, int remaining, long retryAfter) {
        this.allowed = allowed;
        this.remaining = remaining;
        this.retryAfter = retryAfter;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public int getRemaining() {
        return remaining;
    }

    public long getRetryAfter() {
        return retryAfter;
    }
}
