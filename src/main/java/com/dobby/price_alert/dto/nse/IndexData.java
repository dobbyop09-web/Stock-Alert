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

    private Double last;
    private Double variation;
    private Double percentChange;

    private Double open;
    private Double high;
    private Double low;

    private Double previousClose;

    private Double yearHigh;
    private Double yearLow;

    private Double indicativeClose;

    /*
     * NSE sometimes sends these as strings,
     * and sometimes they can be null / "-".
     */
    private String pe;
    private String pb;
    private String dy;

    private String declines;
    private String advances;
    private String unchanged;

    private Double perChange365d;
    private Double perChange30d;

    private String date365dAgo;
    private String date30dAgo;
    private String previousDay;
    private String oneWeekAgo;

    private Double oneMonthAgoVal;
    private Double oneWeekAgoVal;
    private Double oneYearAgoVal;
    private Double previousDayVal;

    private String chart365dPath;
    private String chart30dPath;
    private String chartTodayPath;

}
