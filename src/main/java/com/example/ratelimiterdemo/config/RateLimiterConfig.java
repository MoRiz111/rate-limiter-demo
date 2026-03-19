package com.example.ratelimiterdemo.config;

public class RateLimiterConfig {
    private int requestLimit;
    private int timeWindow;

    public int getRequestLimit() {
        return requestLimit;
    }

    public int getTimeWindow() {
        return timeWindow;
    }

    public RateLimiterConfig(int requestLimit, int timeWindow) {
        super();
        this.requestLimit = requestLimit;
        this.timeWindow = timeWindow;
    }

}
