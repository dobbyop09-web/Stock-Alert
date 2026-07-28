package com.dobby.price_alert.dto.nse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaData {
    private String symbol;
    private BigDecimal open;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal closePrice;
    private BigDecimal previousClose;
    private BigDecimal pChange;
}
