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
public class DashboardStock {
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal alertPrice;
    private BigDecimal distance;
    private String status;
    private String sheet;
}
