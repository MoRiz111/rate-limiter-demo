package com.example.ratelimiterdemo.service;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {
    private final Map<String, Queue<Long>> requestMap = new HashMap<>();

    private static final int REQUEST_LIMIT = 5;
    private static final long TIME_WINDOW = 10000; // 10 seconds

    public boolean allowRequest(final String userId) {
        final long currentTime = System.currentTimeMillis();
        requestMap.putIfAbsent(userId, new LinkedList<Long>());
        Queue<Long> timeStamps = requestMap.get(userId);

        // remove expired timestamps
        while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > TIME_WINDOW) {
            timeStamps.poll();
        }

        if (timeStamps.size() >= REQUEST_LIMIT) {
            return false;
        }

        timeStamps.add(currentTime);
        return true;
    }
}
