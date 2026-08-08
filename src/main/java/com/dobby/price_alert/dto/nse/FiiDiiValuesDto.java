package com.dobby.price_alert.dto.nse;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class FiiDiiValuesDto {
    private BigDecimal buyValue;

    private BigDecimal sellValue;

    private BigDecimal netValue;
}
