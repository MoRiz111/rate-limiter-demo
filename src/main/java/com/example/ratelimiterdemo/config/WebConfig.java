package com.example.ratelimiterdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.ratelimiterdemo.interceptor.RateLimiterInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final RateLimiterInterceptor interceptor;

    public WebConfig(RateLimiterInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
