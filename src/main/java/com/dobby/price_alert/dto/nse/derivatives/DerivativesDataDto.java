package com.dobby.price_alert.dto.nse.derivatives;

import lombok.Data;

@Data
public class DerivativesDataDto {

    private Double change;
    private Double changeinOpenInterest;
    private Double closePrice;
    private String expiryDate;
    private Double highPrice;
    private String identifier;
    private String instrumentType;
    private Double lastPrice;
    private Double lowPrice;
    private Double openInterest;
    private Double openPrice;
    private String optionType;
    private Double pchange;
    private Double pchangeinOpenInterest;
    private Double prevClose;
    private String strikePrice;
    private Double ticksize;
    private Double totalTradedVolume;
    private Double totalTurnover;
    private String underlying;
    private Double underlyingValue;
    private Double volumeFreezeQuantity;
}