package com.dobby.price_alert.dto.bse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Header {

    @JsonProperty("Noticecnt")
    private Integer noticecnt;

    @JsonProperty("PrevClose")
    private BigDecimal prevClose;

    @JsonProperty("Open")
    private BigDecimal open;

    @JsonProperty("High")
    private BigDecimal high;

    @JsonProperty("Low")
    private BigDecimal low;

    @JsonProperty("LTP")
    private BigDecimal ltp;

    @JsonProperty("DisplayText")
    private String displayText;

    @JsonProperty("Category")
    private String category;

    // Pre-open / call-auction block — always null in your sample,
    // so type is unconfirmed. Left as BigDecimal for the numeric-looking
    // ones; revisit once you see a populated response.
    @JsonProperty("PRE_OPEN_NO_BIDS")
    private Integer preOpenNoBids;

    @JsonProperty("PRE_OPEN_NO_I_PRICE")
    private BigDecimal preOpenNoIPrice;

    @JsonProperty("PRE_OPEN_I_PRICE")
    private BigDecimal preOpenIPrice;

    @JsonProperty("PRE_OPEN_I_PRICE_QTY")
    private Integer preOpenIPriceQty;

    @JsonProperty("PCAS_NO_BIDS")
    private Integer pcasNoBids;

    @JsonProperty("PCAS_INDICATIVE_PRICE")
    private BigDecimal pcasIndicativePrice;

    @JsonProperty("PCAS_INDICATIVE_QTY")
    private Integer pcasIndicativeQty;

    @JsonProperty("PERODIC_CALL_AUCTION")
    private String perodicCallAuction;

    @JsonProperty("GSMURL")
    private String gsmurl;

    @JsonProperty("GSMText")
    private String gsmText;

    @JsonProperty("Invit")
    private String invit;

    @JsonProperty("Ason")
    private String ason; // "22 Sep 26 | 16:00" — custom format, not a standard parseable pattern; keep as String or add a custom DateTimeFormatter-based deserializer if you need it as a real timestamp

    @JsonProperty("NAVRate")
    private BigDecimal navRate;

    @JsonProperty("NAVdttm")
    private String navdttm;

    @JsonProperty("ASMText")
    private String asmText;

    @JsonProperty("SMSText")
    private String smsText;

    @JsonProperty("IRPText")
    private String irpText;

    @JsonProperty("ASMURL")
    private String asmurl;

    @JsonProperty("SMSURL")
    private String smsurl;

    @JsonProperty("IRPURL")
    private String irpurl;

    @JsonProperty("IDB_DisplayText")
    private String idbDisplayText;

    @JsonProperty("IsALF")
    private String isALF;

    @JsonProperty("EMSText")
    private String emsText;

    @JsonProperty("EMSURL")
    private String emsurl;
}