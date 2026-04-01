package com.example.ratelimiterdemo.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.example.ratelimiterdemo.dto.RateLimiterResponse;
import com.example.ratelimiterdemo.service.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimiterInterceptor implements HandlerInterceptor {
    private final RateLimiterService rateLimiterService;

    public RateLimiterInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) throws Exception {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE); //automatically maps the correct request in controller
        //It provides the matched request mapping pattern after Spring resolves the controller, allowing us to extract endpoint information in a clean and reliable way.
        System.out.println("Pattern - " + pattern);
        
        String endPoint = "user"; //default feedback
        
        if (pattern != null) {
            String[] parts = pattern.split("/");
            
            if (parts.length > 3) {
                endPoint = parts[3]; //dialogue /scene /etc.
            }
        }
        
        //Extract userId (last segment of URI)
        String uri = request.getRequestURI();
        String[] segments = uri.split("/");
        String userId = segments[segments.length-1];
        
        //Call rate limiter service
        RateLimiterResponse result = rateLimiterService.allowRequest(userId, endPoint);
        
        //Set response headers
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
        
        //If not allowed - block request
        if (!result.isAllowed()) {
            long retryAfterSeconds = result.getRetryAfter();
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.sendError(429, "Rate Limit Exceeded");
            return false;
        }
        
        return true;
                
    }

}
