package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

@Service
public class StockProcessingService {

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final StockAlertService stockAlertService;
    private final TelegramService telegramService;
    private final AlertHistoryService alertHistoryService;

    public StockProcessingService(
            StockAlertService stockAlertService,
            TelegramService telegramService,
            AlertHistoryService alertHistoryService
    ) {
        this.stockAlertService = stockAlertService;
        this.telegramService = telegramService;
        this.alertHistoryService = alertHistoryService;
    }

    public DashboardStock processStock(
            String symbol,
            String companyName,
            String sheet,
            BigDecimal alertPrice,
            MarketData marketData,
            Set<String> triggeredToday,
            Integer sheetRow,
            BigDecimal fib
    ) {

        double current =
                marketData.getCurrentPrice();

        double dayLow =
                marketData.getDayLow();

        double prevClose =
                marketData.getPreviousPrice();

        double marketCap =
                marketData.getMarketCap();

        String screenerUrl =
                "https://www.screener.in/company/"
                        + symbol
                        + "/consolidated";

        AlertStatus alertStatus =
                stockAlertService.shouldSendAlert(
                        sheet,
                        symbol,
                        dayLow,
                        alertPrice.doubleValue(),
                        triggeredToday
                );

        if (alertStatus.isShouldSend()) {

            sendAlert(
                    symbol,
                    sheet,
                    alertPrice,
                    current,
                    dayLow,
                    companyName,
                    screenerUrl
            );
        }

        double distance =
                ((current - alertPrice.doubleValue())
                        / alertPrice.doubleValue()) * 100;

        double changePerc =
                ((current - prevClose)
                        / prevClose) * 100;

        String status =
                calculateStatus(
                        distance,
                        alertStatus
                );

        return DashboardStock.builder()
                .symbol(symbol)
                .companyName(companyName)
                .currentPrice(
                        BigDecimal.valueOf(current)
                )
                .alertPrice(alertPrice)
                .distance(
                        BigDecimal.valueOf(distance)
                )
                .status(status)
                .sheet(sheet)
                .previousClose(
                        BigDecimal.valueOf(prevClose)
                )
                .marketCap(
                        BigDecimal.valueOf(marketCap)
                )
                .changePercent(
                        BigDecimal.valueOf(changePerc)
                )
                .sheetRow(sheetRow)
                .screenerUrl(screenerUrl)
                .fib(fib)
                .dayLow(
                        BigDecimal.valueOf(dayLow)
                )
                .build();
    }

    private void sendAlert(
            String symbol,
            String sheet,
            BigDecimal alertPrice,
            double current,
            double dayLow,
            String companyName,
            String screenerUrl
    ) {

        StockMessageDto dto =
                StockMessageDto.builder()
                        .stockName(symbol)
                        .currentPrice(current)
                        .targetPrice(
                                alertPrice.doubleValue()
                        )
                        .screenerUrl(screenerUrl)
                        .sheetName(sheet)
                        .build();

        String message =
                MessageFormat.format(dto);

        telegramService.sendMessage(message);

        HistoricalAlert historicalAlert =
                HistoricalAlert.builder()
                        .date(
                                LocalDate.now(
                                        INDIA_ZONE
                                ).toString()
                        )
                        .triggeredAt(
                                Instant.now().toString()
                        )
                        .symbol(symbol)
                        .companyName(companyName)
                        .sheet(sheet)
                        .alertPrice(alertPrice)
                        .triggerPrice(
                                BigDecimal.valueOf(dayLow)
                        )
                        .currentPrice(
                                BigDecimal.valueOf(current)
                        )
                        .watchlist(sheet)
                        .screenerUrl(screenerUrl)
                        .build();

        DayOfWeek today =
                LocalDate.now().getDayOfWeek();

        if (today != DayOfWeek.SATURDAY
                && today != DayOfWeek.SUNDAY) {

            alertHistoryService.addAlert(
                    historicalAlert
            );
        }
    }

    private String calculateStatus(
            double distance,
            AlertStatus alertStatus
    ) {

        if (alertStatus.isTriggeredToday()) {
            return "Triggered";
        }

        if (distance <= 5) {
            return "Near";
        }

        if (distance <= 15) {
            return "Watch";
        }

        return "Far";
    }
}