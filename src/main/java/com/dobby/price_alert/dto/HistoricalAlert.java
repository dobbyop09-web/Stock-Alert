package com.dobby.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HistoricalAlert {

    private String date;

    private String triggeredAt;

    private String symbol;

    private String companyName;

    private String sheet;

    private BigDecimal alertPrice;

    private BigDecimal triggerPrice;

    private BigDecimal currentPrice;

    private String watchlist;

    private String screenerUrl;
}