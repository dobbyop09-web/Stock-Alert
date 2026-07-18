package com.dobby.price_alert.service;

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
import java.net.URL;

@Component
public class CsvReaderService {
    @Autowired
    private TelegramService telegramService;

    @Autowired
    private StockAlertService stockAlertService;

    private static final Logger log =
            LoggerFactory.getLogger(CsvReaderService.class);


    public void readCsvAndCheckAlerts(SheetConfig sheetConfig) throws IOException {

        log.info("Reading {}", sheetConfig.getName());

        Reader reader = new InputStreamReader(
                new URL(sheetConfig.getUrl()).openStream());

        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(reader);

        for (CSVRecord record : records) {
            double current =
                    Double.parseDouble(record.get("Current Price"));

            double alert =
                    Double.parseDouble(record.get("Alert Price"));

            double fib = Double.parseDouble(record.get("FIB"));
            String symbol = record.get("Symbol");

            if (stockAlertService.shouldSendAlert(sheetConfig.getName(), symbol, current, alert)) {
                log.info("Telegram sent for {}", symbol);
                String screenerUrl ="https://www.screener.in/company/" + symbol + "/consolidated/";
                StockMessageDto dto = StockMessageDto.builder().stockName(symbol).currentPrice(current).targetPrice(alert).screenerUrl(screenerUrl).build();
                String message = MessageFormat.format(dto);
                telegramService.sendMessage(message);

            }
        }
    }
}
