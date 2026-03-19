package com.example.ratelimiterdemo.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
//@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
    private RateLimiterConfig defaultConfig;
    private Map<String, RateLimiterConfig> endpoints; // if there is any error, interchange these 2 variables position, as that is the order in .properties file

    public RateLimiterConfig getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(RateLimiterConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public Map<String, RateLimiterConfig> getEndpoints() {
        return endpoints;
    }

    public void setEndPoints(Map<String, RateLimiterConfig> endPoints) {
        this.endpoints = endPoints;
    }

}
