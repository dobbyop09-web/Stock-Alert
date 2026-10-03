package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.*;
import com.dobby.price_alert.dto.kotak.KotakQuoteResponse;
import com.dobby.price_alert.service.kotak.KotakMarketDataService;
import com.dobby.price_alert.service.kotak.KotakQuoteService;
import com.dobby.price_alert.service.kotak.KotakScripLookupService;
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
import java.time.ZoneId;
import java.util.*;


@Component
public class CsvReaderService {

    @Autowired
    private MarketDataService marketDataService;

    @Autowired
    private KotakScripLookupService kotakScripLookupService;

    @Autowired
    private KotakQuoteService kotakQuoteService;

    @Autowired
    private KotakMarketDataService kotakMarketDataService;

    @Autowired
    private StockProcessingService stockProcessingService;


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
            if(symbol.equals("NSE")) {
               marketData= kotakMarketDataService.getMarketData("544937");
            }else{
                marketData=  marketDataService.getMarketData(symbol);
            }

            DashboardStock dashboardStock =
                    stockProcessingService.processStock(
                            symbol,
                            marketData.getCompanyName(),
                            sheetConfig.getName(),
                            BigDecimal.valueOf(alert),
                            marketData,
                            triggeredToday,
                            rowNumber,
                            BigDecimal.valueOf(fib)
                    );

            dashboardStocks.add(dashboardStock);
        }

        return dashboardStocks;
    }
    public void testKotakForCsvStocks(
            SheetConfig sheetConfig,Map<String, KotakScrip> scripMap) throws IOException {

        log.info(
                "Reading symbols for Kotak: {}",
                sheetConfig.getName()
        );

        List<String> symbols = new ArrayList<>();

        try (Reader reader = new InputStreamReader(
                new URL(sheetConfig.getUrl()).openStream())) {

            Iterable<CSVRecord> records =
                    CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                            .parse(reader);

            for (CSVRecord record : records) {

                String symbol = record.get("Symbol");

                if (symbol != null && !symbol.isBlank()) {
                    symbols.add(symbol);
                }
            }
        }

        log.info(
                "Found {} symbols in {}",
                symbols.size(),
                sheetConfig.getName()
        );


        Map<String, KotakScrip> matchedScrips =
                kotakScripLookupService.findMatchingScrips(
                        symbols,
                        scripMap
                );

        List<String> kotakTokens =
                matchedScrips.values()
                        .stream()
                        .map(KotakScrip::getSymbol)
                        .toList();

        log.info(
                "Kotak matched {} out of {} symbols",
                matchedScrips.size(),
                symbols.size()
        );

        log.info(
                "Kotak token count: {}",
                kotakTokens.size()
        );
        List<KotakQuoteResponse> quotes =
                kotakQuoteService.getOhlc(
                        "nse_cm",
                        kotakTokens
                );
        log.info("Quotes returned size" + ":"+ quotes.get(0));
        for (KotakQuoteResponse quote : quotes) {

            System.out.println(
                    quote.getDisplaySymbol()
                            + " | LTP: "
                            + quote.getLtp()
                            + " | Change %: "
                            + quote.getPercentageChange()
                            + " | Low: "
                            + quote.getOhlc().getLow()
                            + " | Close: "
                            + quote.getOhlc().getClose()
            );
        }
        // Kotak processing will start here
    }

    public List<DashboardStock> readCsvAndCheckKotakAlerts(
            SheetConfig sheetConfig,
            Set<String> triggeredToday,  Map<String, KotakScrip> scripMap) throws IOException {

        log.info("Reading Kotak data for {}", sheetConfig.getName());

        List<DashboardStock> dashboardStocks =
                new ArrayList<>();

        /*
         * ---------------------------------------------------------
         * 1. Read CSV records
         * ---------------------------------------------------------
         */

        Reader reader = new InputStreamReader(
                new URL(sheetConfig.getUrl()).openStream());

        Iterable<CSVRecord> records =
                CSVFormat.DEFAULT
                        .withFirstRecordAsHeader()
                        .parse(reader);

        List<CSVRecord> csvRecords =
                new ArrayList<>();

        List<String> symbols =
                new ArrayList<>();

        for (CSVRecord record : records) {

            csvRecords.add(record);

            String symbol = record.get("Symbol");

            symbols.add(symbol);
        }

        /*
         * ---------------------------------------------------------
         * 2. Resolve symbols against Kotak master
         * ---------------------------------------------------------
         */

        Map<String, KotakScrip> matchedScrips =
                new HashMap<>();

        for (String symbol : symbols) {
            KotakScrip scrip =
                    scripMap.get(symbol);

            if (scrip != null) {

                matchedScrips.put(
                        symbol,
                        scrip
                );
            } else {

                log.warn(
                        "Kotak scrip not found for symbol: {}",
                        symbol
                );
            }
        }

        /*
         * ---------------------------------------------------------
         * 3. Create token list
         * ---------------------------------------------------------
         */

        List<String> tokens =
                matchedScrips.values()
                        .stream()
                        .map(KotakScrip::getSymbol)
                        .toList();

        log.info(
                "Kotak symbols: {} | matched: {} | tokens: {}",
                symbols.size(),
                matchedScrips.size(),
                tokens.size()
        );

       // log.info("Tokens :"+ tokens );

        /*
         * ---------------------------------------------------------
         * 4. Get Kotak quotes ONCE for this sheet
         * ---------------------------------------------------------
         */

        List<KotakQuoteResponse> quotes =
                kotakQuoteService.getOhlc(
                        "nse_cm",
                        tokens
                );
        /*
         * ---------------------------------------------------------
         * 5. Convert quotes into MarketData
         * ---------------------------------------------------------
         */

        Map<String, MarketData> marketDataMap =
                new HashMap<>();

        for (KotakQuoteResponse quote : quotes) {

            String kotakSymbol =
                    quote.getDisplaySymbol();

            MarketData marketData =
                    kotakMarketDataService
                            .mapToMarketData(quote);
            String symbol = kotakSymbol;

            int dashIndex = symbol.lastIndexOf("-");

            if (dashIndex > 0) {
                symbol = symbol.substring(0, dashIndex);
            }

            marketDataMap.put(
                    symbol,
                    marketData
            );
        }

        /*
         * ---------------------------------------------------------
         * 6. Now process the CSV exactly like your NSE logic
         * ---------------------------------------------------------
         */

        for (CSVRecord record : csvRecords) {

            int rowNumber = (int) record.getRecordNumber() + 1;

            String symbol = record.get("Symbol");

            double alert = Double.parseDouble(record.get("Alert Price"));

            double fib = Double.parseDouble(record.get("FIB"));

            MarketData marketData = marketDataMap.get(symbol);

            if (marketData == null) {
                log.warn("No Kotak market data found for {}", symbol);
                continue;
            }

            DashboardStock dashboardStock =
                    stockProcessingService.processStock(
                            symbol,
                            marketData.getCompanyName(),
                            sheetConfig.getName(),
                            BigDecimal.valueOf(alert),
                            marketData,
                            triggeredToday,
                            rowNumber,
                            BigDecimal.valueOf(fib)
                    );

            dashboardStocks.add(dashboardStock);
        }

        return dashboardStocks;

    }
}
