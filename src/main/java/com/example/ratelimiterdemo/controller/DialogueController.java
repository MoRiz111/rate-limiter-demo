package com.example.ratelimiterdemo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ratelimiterdemo.service.RateLimiterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor //Lombok annotations are not working in eclipse
public class DialogueController {
    private final RateLimiterService rateLimiterService;

    public DialogueController(RateLimiterService rateLimiterService) {
        super();
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/dialogue/{userId}")
    public ResponseEntity getDialogues(@PathVariable String userId) {
        return ResponseEntity.ok("Dialogue for " + userId + " retreived");
    }
    
    @GetMapping("scene/{userId}")
    public ResponseEntity getUserDetails(@PathVariable String userId) {
        return ResponseEntity.ok("User details for " + userId + " retreived");
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity getScenes(@PathVariable String userId) {
        return ResponseEntity.ok("User details for " + userId + " retreived");
    }

}
