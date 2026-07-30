package com.dobby.price_alert.dto.nse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexData {
    private String index;
    private String indexSymbol;
    private double last;
    private double variation;
    private double percentChange;
}
