package com.dobby.price_alert.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockConfig {

    private String symbol;
    private String companyName;
    private BigDecimal alertPrice;
    private String sheet;
    private String screenerUrl;
}