package com.dobby.price_alert.dto.nse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PriceInfo {
    private String yearHighDt;
    private String yearLowDt;
    private Integer yearHigh;
    private Integer yearLow;
}

