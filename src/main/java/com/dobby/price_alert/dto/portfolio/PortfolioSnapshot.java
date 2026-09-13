package com.dobby.price_alert.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PortfolioSnapshot {

    private String date;

    private BigDecimal investedValue;

    private BigDecimal currentValue;

    private BigDecimal profitLoss;

    private BigDecimal profitLossPercent;

    private Integer holdingCount;
}