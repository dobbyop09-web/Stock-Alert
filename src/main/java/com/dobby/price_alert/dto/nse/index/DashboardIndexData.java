package com.dobby.price_alert.dto.nse.index;

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

    private double previousClose;

    private double oneWeekChange;
    private double oneMonthChange;
    private double oneYearChange;

    private double pe;
    private double pb;

    private int advances;
    private int declines;
    private int unchanged;

}
