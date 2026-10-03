package com.dobby.price_alert.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockConfig {

    private String symbol;
    private String companyName;
    private BigDecimal alertPrice;
    private String sheet;
    private String screenerUrl;
}