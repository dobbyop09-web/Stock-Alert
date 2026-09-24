package com.dobby.price_alert.service;

import com.dobby.price_alert.dto.*;
import com.dobby.price_alert.dto.kotak.KotakQuoteResponse;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;


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

    @Autowired
    private KotakScripLookupService kotakScripLookupService;

    @Autowired
    private KotakQuoteService kotakQuoteService;

    @Autowired
    private KotakMarketDataService kotakMarketDataService;


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
            Set<String> triggeredToday) throws IOException {

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

            String symbol =
                    record.get("Symbol");

            symbols.add(symbol);
        }

        /*
         * ---------------------------------------------------------
         * 2. Resolve symbols against Kotak master
         * ---------------------------------------------------------
         */

        Map<String, KotakScrip> scripMap =
                kotakScripLookupService.loadNseScripMap();

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

        log.info(
                "Kotak quotes returned: {}",
                quotes.size()
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
            log.info("kotak symbol : "+ kotakSymbol);
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

            int rowNumber =
                    (int) record.getRecordNumber() + 1;

            String symbol =
                    record.get("Symbol");

            double alert =
                    Double.parseDouble(
                            record.get("Alert Price")
                    );

            double fib =
                    Double.parseDouble(
                            record.get("FIB")
                    );

            MarketData marketData =
                    marketDataMap.get(symbol);

            if (marketData == null) {

                log.warn(
                        "No Kotak market data found for {}",
                        symbol
                );

                continue;
            }

            double current =
                    marketData.getCurrentPrice();

            double dayLow =
                    marketData.getDayLow();

            double prevClose =
                    marketData.getPreviousPrice();

            double marketCap =
                    marketData.getMarketCap();

            String companyName =
                    marketData.getCompanyName();

            /*
             * -----------------------------------------------------
             * Alert logic
             * -----------------------------------------------------
             */

            String screenerUrl =
                    "https://www.screener.in/company/"
                            + symbol
                            + "/consolidated";

            AlertStatus alertStatus =
                    stockAlertService.shouldSendAlert(
                            sheetConfig.getName(),
                            symbol,
                            dayLow,
                            alert,
                            triggeredToday
                    );

            if (alertStatus.isShouldSend()) {

                StockMessageDto dto =
                        StockMessageDto.builder()
                                .stockName(symbol)
                                .currentPrice(current)
                                .targetPrice(alert)
                                .screenerUrl(screenerUrl)
                                .sheetName(sheetConfig.getName())
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
                                .sheet(sheetConfig.getName())
                                .alertPrice(
                                        BigDecimal.valueOf(alert)
                                )
                                .triggerPrice(
                                        BigDecimal.valueOf(dayLow)
                                )
                                .currentPrice(
                                        BigDecimal.valueOf(current)
                                )
                                .watchlist(
                                        sheetConfig.getName()
                                )
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

                log.info(
                        "Historical alert saved: {} - {}",
                        sheetConfig.getName(),
                        symbol
                );
            }

            /*
             * -----------------------------------------------------
             * Dashboard calculations
             * -----------------------------------------------------
             */

            double distance =
                    ((current - alert) / alert) * 100;

            double changePerc =
                    ((current - prevClose)
                            / prevClose) * 100;

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

            /*
             * -----------------------------------------------------
             * Dashboard stock
             * -----------------------------------------------------
             */

            dashboardStocks.add(
                    DashboardStock.builder()
                            .symbol(symbol)
                            .companyName(companyName)
                            .currentPrice(
                                    BigDecimal.valueOf(current)
                            )
                            .alertPrice(
                                    BigDecimal.valueOf(alert)
                            )
                            .distance(
                                    BigDecimal.valueOf(distance)
                            )
                            .status(status)
                            .sheet(sheetConfig.getName())
                            .previousClose(
                                    BigDecimal.valueOf(prevClose)
                            )
                            .marketCap(
                                    BigDecimal.valueOf(marketCap)
                            )
                            .changePercent(
                                    BigDecimal.valueOf(changePerc)
                            )
                            .sheetRow(rowNumber)
                            .screenerUrl(screenerUrl)
                            .fib(
                                    BigDecimal.valueOf(fib)
                            )
                            .build()
            );
        }

        return dashboardStocks;
    }
}
