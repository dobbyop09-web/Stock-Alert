package com.dobby.price_alert.util;

import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class CheckTime {
    public static boolean isAfterMarketHours() {
        LocalTime currentTime = LocalTime.now(
                ZoneId.of("Asia/Kolkata")
        );

        return !currentTime.isBefore(
                LocalTime.of(18, 0)
        );
    }
}
