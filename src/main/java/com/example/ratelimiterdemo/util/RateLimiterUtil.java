package com.example.ratelimiterdemo.util;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Some util function mostly used for testing purpose
 */
public class RateLimiterUtil {
    public static void printRateLimiterMap(Map<String, Queue<Long>> requestMap) {
        requestMap.forEach((user, timeStamps) -> {
            List<String> formatedTimestamps = timeStamps.stream()
                    .map(timeStamp -> convertMillisToHumanReadableFormat(timeStamp, "HH:mm:ss")).toList();
            System.out.println(user + " - " + formatedTimestamps);
        });
    }

    public static String convertMillisToHumanReadableFormat(long millis, String timeFormat) {
        return new SimpleDateFormat(timeFormat).format(millis);
    }

}
