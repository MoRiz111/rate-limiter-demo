package com.example.ratelimiterdemo.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.ratelimiterdemo.config.RateLimiterConfig;
import com.example.ratelimiterdemo.config.RateLimiterProperties;
import com.example.ratelimiterdemo.util.RateLimiterUtil;

@Service
public class RateLimiterService {
    private final Map<String, Queue<Long>> requestMap = new ConcurrentHashMap<>();
    final Map<String, RateLimiterConfig> timeWindowMap = new HashMap<>();

    private final RateLimiterProperties properties;

    public RateLimiterService(RateLimiterProperties properties) {
        this.properties = properties;

        timeWindowMap.put("default", new RateLimiterConfig(3, 5000));
        timeWindowMap.put("dialogue", new RateLimiterConfig(4, 5000));
        timeWindowMap.put("user", new RateLimiterConfig(2, 10000));
    }

    private RateLimiterConfig resolveConfig(String endpoint) {
        return properties.getEndpoints().getOrDefault(endpoint, properties.getDefaultConfig());
    }

//    commented as these properties are made dynamic
//    private static final int REQUEST_LIMIT = 5;
//    private static final long TIME_WINDOW = 10000; // 10 seconds

    /**
     * Requests are filtered by a combination of endpoint and userId
     * I can also add other filter say user only (sometimes user may use different end points for malicious attacks),
     * or location based, in those I can keep adding similar 'allowRequest()' and only if it passes all, it reaches the controller
     * 
     * @param rateLimitKey
     * @param endPoint
     * @return
     */
    public boolean allowRequest(final String rateLimitKey, final String endPoint) {
        // RateLimiterConfig config = resolveConfig(endPoint);

        final RateLimiterConfig rateLimiterConfigForEndPoint = timeWindowMap.getOrDefault(endPoint, timeWindowMap.get("default"));
        final int requestLimit = rateLimiterConfigForEndPoint.getRequestLimit();
        final long timeWindow = rateLimiterConfigForEndPoint.getTimeWindow();

        final long currentTime = System.currentTimeMillis();

        requestMap.putIfAbsent(rateLimitKey, new ConcurrentLinkedDeque<>());
        Queue<Long> timeStamps = requestMap.get(rateLimitKey);

        synchronized (timeStamps) {
            // critical block
            // remove expired timestamps
            while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > timeWindow) {
                timeStamps.poll();
            }

            if (timeStamps.size() >= requestLimit) {
                return false;
            }

            timeStamps.add(currentTime);
            return true;
        }
    }

    /**
     * Clean up the older unused users, to clear up space
     */
    @Scheduled(fixedRate = 10000) // runs for every minute
    public void cleanUp() {
        long currentTime = System.currentTimeMillis();

        System.out.println("Before clean up : ");
        RateLimiterUtil.printRateLimiterMap(requestMap);

        requestMap.entrySet().removeIf(entry -> {
            String rateLimitKey = entry.getKey();
            String[] parts = rateLimitKey.split(":");
            String endPoint = parts.length > 1 ? parts[1] : "default";

//            RateLimiterConfig config = resolveConfig(endPoint);
            final RateLimiterConfig rateLimiterConfigForEndPoint = timeWindowMap.getOrDefault(endPoint, timeWindowMap.get("default"));
            long timeWindow = rateLimiterConfigForEndPoint.getTimeWindow();

            Queue<Long> timeStamps = entry.getValue();

            // synchronized block is used, so that the queue is in sync with the
            // allowRequest()
            synchronized (timeStamps) {
                while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > timeWindow) {
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
