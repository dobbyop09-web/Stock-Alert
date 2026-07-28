package com.dobby.price_alert.dto.nse;

import lombok.Data;

@Data
public class EquityResponse {
    private MetaData metaData;
    private PriceInfo priceInfo;
    private TradeInfo tradeInfo;
    private String lastUpdateTime;
}
