package com.dobby.price_alert.dto.nse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FiiDiiHistoryDto {
    @JsonFormat(pattern = "dd-MMM-yyyy", locale = "en")
    private LocalDate date;

    private FiiDiiValuesDto fii;

    private FiiDiiValuesDto dii;
}
