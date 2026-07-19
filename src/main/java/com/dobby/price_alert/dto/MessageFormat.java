package com.dobby.price_alert.dto;

public class MessageFormat {
    public static String format(StockMessageDto dto){
        return String.format(
                """
                📈 STOCK ALERT

                🏢 Stock : %s
                📂 Sheet : %s
                💰 Price : ₹%s
                🎯 Alert Price : ₹%s
                🔗 Screener
                %s
                """,
                dto.getStockName(),
                dto.getSheetName(),
                dto.getCurrentPrice(),
                dto.getTargetPrice(),
                dto.getScreenerUrl());
    }
}
