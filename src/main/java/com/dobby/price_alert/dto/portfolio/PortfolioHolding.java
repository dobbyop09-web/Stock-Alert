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
public class PortfolioHolding {

    private String symbol;

    private Integer quantity;

    private BigDecimal buyPrice;

    private String buyDate;

    private BigDecimal targetAmount;

    private BigDecimal currentPrice;

    private String sellDate;
}
