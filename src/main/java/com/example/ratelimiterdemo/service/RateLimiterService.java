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
import com.example.ratelimiterdemo.dto.RateLimiterResponse;
import com.example.ratelimiterdemo.util.RateLimiterUtil;

@Service
public class RateLimiterService {
    private final Map<String, Queue<Long>> userIdRequestMap = new ConcurrentHashMap<>();
    private final Map<String, Queue<Long>> endPointRequestMap = new ConcurrentHashMap<>();
    final Map<String, RateLimiterConfig> timeWindowConfigMap = new HashMap<>();
    
    //this is request limits for users, here it is hardcoded, 
    //but can be made to be fetched from a config file
    private final int REQUEST_LIMIT_PER_USER = 5;
    private final long TIME_WINDOW_PER_USER = 10000;

    private final RateLimiterProperties properties;

    public RateLimiterService(RateLimiterProperties properties) {
        this.properties = properties;

        timeWindowConfigMap.put("default", new RateLimiterConfig(3, 5000));
        timeWindowConfigMap.put("dialogue", new RateLimiterConfig(4, 5000));
        timeWindowConfigMap.put("user", new RateLimiterConfig(2, 10000));
    }

    private RateLimiterConfig resolveConfig(String endpoint) {
        return properties.getEndpoints().getOrDefault(endpoint, properties.getDefaultConfig());
    }
    
    /**
     * This is the main allow request, returns true only if all the rate limits are satisfied
     * 
     * @param userId
     * @param endPoint
     * @return
     */
    public RateLimiterResponse allowRequest(final String userId, final String endPoint) {
        final String rateLimitKey = endPoint + ":" + userId;
        
        RateLimiterResponse userIdRateLimitResponse = allowRequestByUserId(userId);
        
        if (!userIdRateLimitResponse.isAllowed()) {
            return userIdRateLimitResponse;
        }
        
        //when both rate limiter passed, return a combined success
        RateLimiterResponse endPointRateLimitResponse = allowRequestByEndPoint(rateLimitKey, endPoint);
        
        //Math.min is used so that client sees the stricter rate limit, as if that is hit, API call will be blocked
        return new RateLimiterResponse(true,
                Math.min(userIdRateLimitResponse.getRemaining(), endPointRateLimitResponse.getRemaining()), 0);
    }

    /**
     * Filters request based on individual user's rate limit
     * 
     * @param userId
     * @return
     */
    public RateLimiterResponse allowRequestByUserId(final String userId) {
        final long currentTime = System.currentTimeMillis();
        userIdRequestMap.putIfAbsent(userId, new ConcurrentLinkedDeque<>());
        Queue<Long> timeStamps = userIdRequestMap.get(userId);


        synchronized (timeStamps) {
            // critical block
            // remove expired timestamps
            while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > TIME_WINDOW_PER_USER) {
                timeStamps.poll();
            }

            if (timeStamps.size() >= REQUEST_LIMIT_PER_USER) {
                //this value is given in seconds
                long retryAfter = (TIME_WINDOW_PER_USER - (currentTime - timeStamps.peek()))/1000;
                
                //remaining no. of hits is '0', as the request limit is already hit.
                return new RateLimiterResponse(false, 0, retryAfter);
            }

            System.out.println(userId + "-" + timeStamps);
            timeStamps.add(currentTime);
            int remaining = REQUEST_LIMIT_PER_USER - timeStamps.size();
            
            //retry after time is set '0', as currently there is no restriction, as method is allowed now.
            return new RateLimiterResponse(true, remaining, 0);
        }
    }

    /**
     * Requests are filtered by a combination of endpoint and userId
     * I can also add other filter say user only (sometimes user may use different end points for malicious attacks),
     * or location based, in those I can keep adding similar 'allowRequest()' and only if it passes all, it reaches the controller
     * 
     * @param rateLimitKey
     * @param endPoint
     * @return
     */
    public RateLimiterResponse allowRequestByEndPoint(final String rateLimitKey, final String endPoint) {
        // RateLimiterConfig config = resolveConfig(endPoint);

        final RateLimiterConfig rateLimiterConfigForEndPoint = timeWindowConfigMap.getOrDefault(endPoint, timeWindowConfigMap.get("default"));
        final int requestLimit = rateLimiterConfigForEndPoint.getRequestLimit();
        final long timeWindow = rateLimiterConfigForEndPoint.getTimeWindow();

        final long currentTime = System.currentTimeMillis();

        endPointRequestMap.putIfAbsent(rateLimitKey, new ConcurrentLinkedDeque<>());
        Queue<Long> timeStamps = endPointRequestMap.get(rateLimitKey);
        
        System.out.println("endPointRequestMap - Before hitting allow request : ");
        RateLimiterUtil.printRateLimiterMap(endPointRequestMap);

        synchronized (timeStamps) {
            // critical block
            // remove expired timestamps
            while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > timeWindow) {
                timeStamps.poll();
            }

            if (timeStamps.size() >= requestLimit) {
                long retryAfter = (timeWindow - (currentTime - timeStamps.peek()))/1000;
                
                return new RateLimiterResponse(false, 0, retryAfter);
            }

            timeStamps.add(currentTime);
            int remaining = requestLimit - timeStamps.size();
            
            System.out.println("endPointRequestMap - After hitting allow request : ");
            RateLimiterUtil.printRateLimiterMap(endPointRequestMap);
            
            //retry after time is set '0', as currently there is no restriction, as method is allowed now.
            return new RateLimiterResponse(true, remaining, 0);
        }
    }

    /**
     * Clean up the older unused users, to clear up space
     */
    @Scheduled(fixedRate = 10000) // runs for every minute
    private void cleanUp() {
        long currentTime = System.currentTimeMillis();

//        cleaning up the userIdRequestMap
        System.out.println("endPointRequestMap - Before clean up : ");
        RateLimiterUtil.printRateLimiterMap(endPointRequestMap);

        
        userIdRequestMap.entrySet().removeIf(entry -> {
            String rateLimitKey = entry.getKey();
            String[] segments = rateLimitKey.split(":");
            String endPoint = null;
            
            
            if (segments.length >= 5) {
                // api/user /dialogue/1 or /scene/1
                endPoint = segments[3];
            } else if (segments.length == 4) {
             // api/user /1
                endPoint = "user";
            }

//          RateLimiterConfig config = resolveConfig(endPoint);
            final RateLimiterConfig rateLimiterConfigForEndPoint = timeWindowConfigMap.getOrDefault(endPoint, timeWindowConfigMap.get("default"));
            long timeWindow = rateLimiterConfigForEndPoint.getTimeWindow();

            Queue<Long> timeStamps = entry.getValue();

            // synchronized block is used, so that the queue is in sync with the
            // allowRequest()
            synchronized (timeStamps) {
                while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > timeWindow) {
                    timeStamps.poll();
                }

                // remove the <k,v>  pair only if the queue becomes empty
                return timeStamps.isEmpty();
            }
        });

        System.out.println("endPointRequestMap - After clean up ("
                + RateLimiterUtil.convertMillisToHumanReadableFormat(currentTime, "HH:mm:ss") + ") : ");
        RateLimiterUtil.printRateLimiterMap(userIdRequestMap);
        System.out.println();
        
        cleanUpUserIdRequestMap();
    }
    
    private void cleanUpUserIdRequestMap() {
        long currentTime = System.currentTimeMillis();
        System.out.println("userIdRequestMap - Before clean up : ");
        RateLimiterUtil.printRateLimiterMap(userIdRequestMap);

        
        userIdRequestMap.entrySet().removeIf(entry -> {
            Queue<Long> timeStamps = entry.getValue();

            // synchronized block is used, so that the queue is in sync with the
            // allowRequest()
            synchronized (timeStamps) {
                while (!timeStamps.isEmpty() && currentTime - timeStamps.peek() > TIME_WINDOW_PER_USER) {
                    timeStamps.poll();
                }

                // remove the user only if the queue becomes empty
                return timeStamps.isEmpty();
            }
        });

        System.out.println("userIdRequestMap - After clean up ("
                + RateLimiterUtil.convertMillisToHumanReadableFormat(currentTime, "HH:mm:ss") + ") : ");
        RateLimiterUtil.printRateLimiterMap(userIdRequestMap);
        System.out.println();
    }
}
