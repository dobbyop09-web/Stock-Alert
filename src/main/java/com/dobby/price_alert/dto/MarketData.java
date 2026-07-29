package com.dobby.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketData {
    private  double currentPrice;
    private  double marketCap;
    private  double previousPrice;
    private  double changePercent;
    private  double dayLow;
}
