package com.dobby.price_alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsvRecordDto {
    private String symbol;
    private double alertPrice;
    private double fib;
    private int rowNumber;
    private String sheetName;
}
