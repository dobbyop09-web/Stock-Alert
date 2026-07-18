package com.dobby.price_alert.dto;

public class MessageFormat {
    public static String format(StockMessageDto dto){
        return String.format(
                """
                📈 STOCK ALERT

                🏢 Stock : %s
                💰 Price : ₹%s
                🎯 Alert Price : ₹%s
                🔗 Screener
                %s
                """,
                dto.getStockName(),
                dto.getCurrentPrice(),
                dto.getTargetPrice(),
                dto.getScreenerUrl());
    }
    public static String format2(StockMessageDto dto){
        return String.format(
                """
                📈 STOCK ALERT
                🏢 Stock : %s
                ⚠️ UpdateAlert : %s

                """,
                dto.getStockName(),
                dto.getUpdateAlert()
        );
    }
}
