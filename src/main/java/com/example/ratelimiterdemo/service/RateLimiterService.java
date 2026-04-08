package com.example.ratelimiterdemo.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.ratelimiterdemo.config.RateLimiterConfig;
import com.example.ratelimiterdemo.dto.RateLimiterResponse;

@Service
public class RateLimiterService {
    private final Map<String, RateLimiterConfig> timeWindowConfigMap = new HashMap<>();

    // this is request limits for users, here it is hardcoded,
    // but can be made to be fetched from a config file
    private final int REQUEST_LIMIT_PER_USER = 5;
    private final long TIME_WINDOW_PER_USER = 10000;

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        timeWindowConfigMap.put("default", new RateLimiterConfig(3, 5000));
        timeWindowConfigMap.put("dialogue", new RateLimiterConfig(4, 5000));
        timeWindowConfigMap.put("user", new RateLimiterConfig(2, 10000));
    }

    /**
     * This is the main allow request, returns true only if all the rate limits are
     * satisfied
     * 
     * @param userId
     * @param endPoint
     * @return
     */
    public RateLimiterResponse allowRequest(final String userId, final String endPoint) {
        RateLimiterResponse userIdRateLimitResponse = allowRequestByUserId(userId);

        if (!userIdRateLimitResponse.isAllowed()) {
            return userIdRateLimitResponse;
        }

        // when both rate limiter passed, return a combined success
        RateLimiterResponse endPointRateLimitResponse = allowRequestByEndPoint(userId, endPoint);

        // Math.min is used so that client sees the stricter rate limit, as if that is
        // hit, API call will be blocked
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
        return checkSlidingWindow("rate_limiter:user:" + userId,
                REQUEST_LIMIT_PER_USER, TIME_WINDOW_PER_USER);
    }
    
    /**
     * Filters request based on individual user's rate limit
     * 
     * @param userId
     * @return
     */
    public RateLimiterResponse allowRequestByEndPoint(final String userId, final String endPoint) {
        return checkSlidingWindow("rate_limiter:endpoint:" + endPoint + ":user:" + userId,
                timeWindowConfigMap.getOrDefault(endPoint, timeWindowConfigMap.get("default")).getRequestLimit(),
                timeWindowConfigMap.getOrDefault(endPoint, timeWindowConfigMap.get("default")).getTimeWindow());
    }

    /**
     * Sliding window using z-set
     * 
     * @param key
     * @param limit
     * @param timeWindowInMillis
     * @return
     */
    private RateLimiterResponse checkSlidingWindow(String key, int limit, long timeWindowInMillis) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - timeWindowInMillis;

        // removed expired entries
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // count current request
        Long count = redisTemplate.opsForZSet().zCard(key);

        if (count != null && count >= limit) {
            // get oldest timestamp
            Set<String> oldestEntry = redisTemplate.opsForZSet().range(key, 0, 0);

            long oldestTime = (oldestEntry != null && !oldestEntry.isEmpty())
                    ? Long.parseLong(oldestEntry.iterator().next())
                    : currentTime;
            long retryAfterInMillis = timeWindowInMillis - (currentTime - oldestTime);

            // remaining no. of hits is '0', as the request limit is already hit.
            return new RateLimiterResponse(false, 0, retryAfterInMillis/1000);
        }

        // add current request's timestamp
        redisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);

        // add expiry time (auto-cleanup)
        redisTemplate.expire(key, timeWindowInMillis, TimeUnit.MILLISECONDS);

        int remainingHits = limit - (count != null ? count.intValue() : 0) - 1;

        // retry after time is set '0', as currently there is no restriction, as method
        // is allowed now.
        return new RateLimiterResponse(true, remainingHits, 0);
    }
}
