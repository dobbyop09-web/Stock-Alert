package com.dobby.price_alert.dto.nse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class DashboardIndexData {

    private String name;
    private String screenerUrl;
    private double last;
    private double variation;
    private double percentChange;

}
