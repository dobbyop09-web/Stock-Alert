package com.dobby.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketData {
    private  double currentPrice;
    private  double marketCap;
    private  double previousPrice;
    private  double changePercent;
}
