package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@Component
public class CsvReaderService {
    @Autowired
    private TelegramService telegramService;

    @Autowired
    private StockAlertService stockAlertService;

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private AlertHistoryService alertHistoryService;

    private static final ZoneId INDIA_ZONE =
            ZoneId.of("Asia/Kolkata");
    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);


    public List<DashboardStock> readCsvAndCheckAlerts(SheetConfig sheetConfig, Set<String> triggeredToday) throws IOException {

        log.info("Reading {}", sheetConfig.getName());
        List<DashboardStock> dashboardStocks = new ArrayList<>();

        Reader reader = new InputStreamReader(
                new URL(sheetConfig.getUrl()).openStream());

        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);

        for (CSVRecord record : records) {
            int rowNumber = (int)record.getRecordNumber()+1;
            String symbol = record.get("Symbol");
            double alert = Double.parseDouble(record.get("Alert Price"));
            double fib = Double.parseDouble(record.get("FIB"));
            MarketData marketData = new MarketData();
            if(!(symbol.equals("544224") || symbol.equals("526433"))) {
              marketData=  marketDataService.getMarketData(symbol);
            }
            String companyName = marketData.getCompanyName();
            double dayLow = marketData.getDayLow();
            double current =marketData.getCurrentPrice();
            double prevClose = marketData.getPreviousPrice();
            double marketCap = marketData.getMarketCap();

            String screenerUrl ="https://www.screener.in/company/" + symbol + "/consolidated";
            AlertStatus  alertStatus = stockAlertService.shouldSendAlert(sheetConfig.getName(), symbol, dayLow, alert,triggeredToday);
            if (alertStatus.isShouldSend()) {
                StockMessageDto dto = StockMessageDto.builder().stockName(symbol).currentPrice(current).targetPrice(alert).screenerUrl(screenerUrl).sheetName(sheetConfig.getName()).build();
                String message = MessageFormat.format(dto);
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

                                .sheet(
                                        sheetConfig.getName()
                                )

                                .alertPrice(
                                        BigDecimal.valueOf(
                                                alert
                                        )
                                )

                                /*
                                 * The alert was triggered because
                                 * dayLow reached/broke the alert price.
                                 */
                                .triggerPrice(
                                        BigDecimal.valueOf(
                                                dayLow
                                        )
                                )

                                .currentPrice(
                                        BigDecimal.valueOf(
                                                current
                                        )
                                )

                                .watchlist(
                                        sheetConfig.getName()
                                )

                                .screenerUrl(
                                        screenerUrl
                                )

                                .build();

                DayOfWeek today = LocalDate.now().getDayOfWeek();

                if (today != DayOfWeek.SATURDAY && today != DayOfWeek.SUNDAY) {
                    alertHistoryService.addAlert(
                            historicalAlert
                    );
                }

                log.info(
                        "Historical alert saved: {} - {}",
                        sheetConfig.getName(),
                        symbol
                );


            }
            double distance = ((current - alert) / alert) * 100;
            double changePerc = ((current-prevClose)/prevClose)*100;
            String status;

            if (alertStatus.isTriggeredToday()) {
                status = "Triggered";
            } else if (distance <= 5) {
                status = "Near";
            } else if (distance <= 15) {
                status = "Watch";
            } else {
                status = "Far";
            }

            dashboardStocks.add(
                    DashboardStock.builder()
                            .symbol(symbol)
                            .companyName(companyName)
                            .currentPrice(BigDecimal.valueOf(current))
                            .alertPrice(BigDecimal.valueOf(alert))
                            .distance(BigDecimal.valueOf(distance))
                            .status(status)
                            .sheet(sheetConfig.getName())
                            .previousClose(BigDecimal.valueOf(prevClose))
                            .marketCap(BigDecimal.valueOf(marketCap))
                            .changePercent(BigDecimal.valueOf(changePerc))
                            .sheetRow(rowNumber)
                            .screenerUrl(screenerUrl)
                            .fib(BigDecimal.valueOf(fib))
                            .build()
            );
        }
        return dashboardStocks;
    }
}
