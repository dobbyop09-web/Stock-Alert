package com.dobby.price_alert.dto.kotak;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class KotakQuoteResponse {
    @JsonProperty("exchange_token")
    private String exchangeToken;

    @JsonProperty("display_symbol")
    private String displaySymbol;

    private String exchange;
    private String ltp;
    private String change;

    @JsonProperty("per_change")
    private String percentageChange;

    private Ohlc ohlc;

    public String getExchangeToken() {
        return exchangeToken;
    }

    public String getDisplaySymbol() {
        return displaySymbol;
    }

    public String getExchange() {
        return exchange;
    }

    public String getLtp() {
        return ltp;
    }

    public String getChange() {
        return change;
    }

    public String getPercentageChange() {
        return percentageChange;
    }

    public Ohlc getOhlc() {
        return ohlc;
    }

    public static class Ohlc {

        private String open;
        private String high;
        private String low;
        private String close;

        public String getOpen() {
            return open;
        }

        public String getHigh() {
            return high;
        }

        public String getLow() {
            return low;
        }

        public String getClose() {
            return close;
        }
    }
}
