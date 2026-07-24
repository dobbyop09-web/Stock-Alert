package com.dobby.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class AlertStatus {
    private boolean shouldSend;
    private boolean triggeredToday;
}
