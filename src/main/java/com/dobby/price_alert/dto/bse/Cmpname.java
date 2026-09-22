package com.dobby.price_alert.dto.bse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Cmpname {

    @JsonProperty("FullN")
    private String fullN;

    @JsonProperty("ShortN")
    private String shortN;

    @JsonProperty("SeriesN")
    private String seriesN;

    @JsonProperty("SEOUrlEQ")
    private String seoUrlEQ;

    @JsonProperty("SEOUrlDR")
    private String seoUrlDR;

    @JsonProperty("IsNotPropernFIT")
    private String isNotPropernFIT;

    @JsonProperty("Category")
    private String category;

    @JsonProperty("EquityScrips")
    private String equityScrips;
}