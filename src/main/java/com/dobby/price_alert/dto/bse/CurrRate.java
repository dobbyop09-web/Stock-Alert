package com.dobby.price_alert.dto.bse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CurrRate {

    @JsonProperty("LTP")
    private BigDecimal ltp;

    @JsonProperty("Chg")
    private BigDecimal chg;

    @JsonProperty("PcChg")
    private BigDecimal pcChg;

    @JsonProperty("D_Cpricelink")
    private String dCpricelink; // likely a URL/link field, stays String

    @JsonProperty("IssueChgVal")
    private BigDecimal issueChgVal;

    @JsonProperty("IssueChgPC")
    private BigDecimal issueChgPC;
}