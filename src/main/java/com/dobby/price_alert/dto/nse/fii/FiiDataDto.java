package com.dobby.price_alert.dto.nse.fii;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FiiDataDto {
    private BigDecimal buyValue;

    private String category;

    @JsonFormat(
            pattern = "dd-MMM-yyyy",
            locale = "en"
    )
    private LocalDate date;

    private BigDecimal netValue;

    private BigDecimal sellValue;
}
