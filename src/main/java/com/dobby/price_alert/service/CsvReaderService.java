package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.DashboardStock;
import com.dobby.price_alert.dto.MessageFormat;
import com.dobby.price_alert.dto.SheetConfig;
import com.dobby.price_alert.dto.StockMessageDto;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvReaderService {
    @Autowired
    private TelegramService telegramService;

    @Autowired
    private StockAlertService stockAlertService;

    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);


    public List<DashboardStock> readCsvAndCheckAlerts(SheetConfig sheetConfig) throws IOException {

        log.info("Reading {}", sheetConfig.getName());
        List<DashboardStock> dashboardStocks = new ArrayList<>();

        Reader reader = new InputStreamReader(
                new URL(sheetConfig.getUrl()).openStream());

        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);

        for (CSVRecord record : records) {
            double current =Double.parseDouble(record.get("Current Price"));
            double alert = Double.parseDouble(record.get("Alert Price"));
            double fib = Double.parseDouble(record.get("FIB"));

            String symbol = record.get("Symbol");


            if (stockAlertService.shouldSendAlert(sheetConfig.getName(), symbol, current, alert)) {
                log.info("Telegram sent for {}", symbol);
                String screenerUrl ="https://www.screener.in/company/" + symbol + "/consolidated/";
                StockMessageDto dto = StockMessageDto.builder().stockName(symbol).currentPrice(current).targetPrice(alert).screenerUrl(screenerUrl).sheetName(sheetConfig.getName()).build();
                String message = MessageFormat.format(dto);
                telegramService.sendMessage(message);

            }
            double distance = ((current - alert) / current) * 100;

            String status;

            if (distance <= 0) {
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
                            .currentPrice(BigDecimal.valueOf(current))
                            .alertPrice(BigDecimal.valueOf(alert))
                            .distance(BigDecimal.valueOf(distance))
                            .status(status)
                            .sheet(sheetConfig.getName())
                            .build()
            );
        }
        return dashboardStocks;
    }
}
