package com.dobby.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AlertHistory {

    @Builder.Default
    private List<HistoricalAlert> alerts = new ArrayList<>();

    private String lastUpdated;
}