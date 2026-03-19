package com.example.ratelimiterdemo.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.example.ratelimiterdemo.service.RateLimiterService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimiterFilter implements Filter {
    private final RateLimiterService rateLimiterService;
    
    public RateLimiterFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        final String uri = httpServletRequest.getRequestURI();
        final String[] segments = uri.split("/");
        
        String userId = null;
        String endPoint = null;
        
        if (segments.length >= 5) {
            // api/user /dialogue/1 or /scene/1
            endPoint = segments[3];
            userId = segments[4];
        } else if (segments.length == 4) {
         // api/user /1
            endPoint = "user";
            userId = segments[3];
        }
        
        if (userId == null) {
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing User Id");
            return;
        }
        
        boolean allowed = rateLimiterService.allowRequest(endPoint + "-" + userId, endPoint);
        
        if (!allowed) {
            //httpServletResponse.sendError(429, "Rate Limit Exceeded"); //simply returning the error, but in some cases APIs are required to return a JSON
            httpServletResponse.setStatus(429);
            httpServletResponse.getWriter().write("{\"error\" : \"Rate Limit Exceeded - Too many request for " + userId + ", please try again  later.\"}");
            return;
        }
        
        //only if this executes, client request goes to the controller (via next filter and dispatcher servlet), else response is returned by tomcat
        chain.doFilter(httpServletRequest, httpServletResponse);
    }

}
