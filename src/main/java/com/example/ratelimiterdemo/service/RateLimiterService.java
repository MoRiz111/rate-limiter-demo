package com.example.ratelimiterdemo.service;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.ratelimiterdemo.util.RateLimiterUtil;

@Service
public class RateLimiterService {
    private final Map<String, Queue<Long>> requestMap = new ConcurrentHashMap<>();

    private static final int REQUEST_LIMIT = 5;
    private static final long TIME_WINDOW = 10000; // 10 seconds

    public boolean allowRequest(final String userId) {
        final long currentTime = System.currentTimeMillis();
        requestMap.putIfAbsent(userId, new ConcurrentLinkedDeque<>());
        Queue<Long> timeStamps = requestMap.get(userId);

        synchronized (timeStamps) {
            // critical block
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

    /**
     * Clean up the older unused users, to clear up space
     */
    @Scheduled(fixedRate = 60000) //runs for every minute
    public void cleanUp() {
        long currentTime = System.currentTimeMillis();

        System.out.println("Before clean up : ");
        RateLimiterUtil.printRateLimiterMap(requestMap);

        requestMap.entrySet().removeIf(entry -> {
            Queue<Long> timeStamps = entry.getValue();

            // synchronized block is used, so that the queue is in sync with the allowRequest()
            synchronized (timeStamps) {
                while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > TIME_WINDOW) {
                    timeStamps.poll();
                }

                // remove the user only if the queue becomes empty
                return timeStamps.isEmpty();
            }
        });

        System.out.println("After clean up ("
                + RateLimiterUtil.convertMillisToHumanReadableFormat(currentTime, "HH:mm:ss") + ") : ");
        RateLimiterUtil.printRateLimiterMap(requestMap);
        System.out.println();
    }
}
