package com.dobby.price_alert.dto.nse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeInfo {
    private BigDecimal totalTradedVolume;
    private BigDecimal totalTradedValue;
    private BigDecimal totalMarketCap;
    private BigDecimal lastPrice;
}
