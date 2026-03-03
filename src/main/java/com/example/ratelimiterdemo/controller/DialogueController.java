package com.example.ratelimiterdemo.controller;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ratelimiterdemo.service.RateLimiterService;

import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dialogue")
@RequiredArgsConstructor //Lombok annotations are not working in eclipse
public class DialogueController {
    private final RateLimiterService rateLimiterService;

    public DialogueController(RateLimiterService rateLimiterService) {
        super();
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity getDialogues(@PathVariable String userId) {
        if (rateLimiterService.allowRequest(userId)) {
            return ResponseEntity.ok("Dialogue for " + userId + " retreived");
        } else {
            return ResponseEntity.status(HttpStatusCode.valueOf(429))
                    .body("Too many request for " + userId + ", please try again  later");
        }
    }

}
