package com.dobby.price_alert.dto;

public class KotakScrip {
    private String symbol;
    private String exchangeSegment;
    private String symbolName;
    private String tradingSymbol;
    private String isin;

    public KotakScrip(
            String symbol,
            String exchangeSegment,
            String symbolName,
            String tradingSymbol,
            String isin) {

        this.symbol = symbol;
        this.exchangeSegment = exchangeSegment;
        this.symbolName = symbolName;
        this.tradingSymbol = tradingSymbol;
        this.isin = isin;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getExchangeSegment() {
        return exchangeSegment;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public String getTradingSymbol() {
        return tradingSymbol;
    }

    public String getIsin() {
        return isin;
    }
}
